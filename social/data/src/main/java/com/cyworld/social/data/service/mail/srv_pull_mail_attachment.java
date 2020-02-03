package com.cyworld.social.data.service.mail;

import akka.actor.ActorRef;
import com.cyworld.social.data.actors.mail.actor_mailbox;
import com.cyworld.social.data.actors.akka_system;
import com.cyworld.social.data.actors.public_message_define;
import com.cyworld.social.data.entity.mail.mail_attachment;
import com.cyworld.social.data.entity.mail.mail_by_personal;
import com.cyworld.social.data.entity.mail.mailbox;
import com.cyworld.social.data.mongodb.db_context;
import com.cyworld.social.utils.request.mail.req_pull_mail_attachment;
import lombok.Data;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import static akka.pattern.Patterns.ask;

@Component
public class srv_pull_mail_attachment {

    @Autowired
    akka_system akka;
    @Autowired
    db_context mongo_context;

    @Data
    public static class context {
        req_pull_mail_attachment req;
        ActorRef actor_mailbox_ref;
    }

    public Mono serv(ServerRequest request) {
        context this_context = new context();
        return request.bodyToMono(req_pull_mail_attachment.class)
                .doOnNext(this_context::setReq)
                .flatMap(body -> {
                    return actor_mailbox.find_or_build(
                            body.getNamesapce(),
                            body.getId(),
                            akka.actor_system);
                })
                .doOnNext(this_context::setActor_mailbox_ref)
                .flatMap(ref -> {
                    if (ref.isTerminated())
                        return Mono.error(new Throwable("cant create actor".concat(ref.path().name())));
                    else
                        return Mono.fromCompletionStage
                                (
                                        ask
                                                (
                                                        ref,
                                                        ((actor_mailbox.msg_inject_ask<mailbox, List<mail_attachment>>)box -> {
                                                            List<mail_attachment> res=new ArrayList<mail_attachment>();
                                                            for (mail_by_personal m : box.getInbox()) {
                                                                if (!m.getId().equals(this_context.req.getMail_id()))
                                                                    continue;
                                                                m.set_read(true);
                                                                for (mail_attachment a : m.getAttachment()) {
                                                                    if (this_context.req.getAttachment_id().equals(a.getId())
                                                                            || this_context.req.getAttachment_id().equals("all")) {
                                                                        if (!a.isRead()) {
                                                                            a.setRead(true);
                                                                            res.add(mail_attachment.deep_clone(a));
                                                                        }
                                                                    }
                                                                }
                                                            }
                                                            return res;
                                                        }),
                                                        Duration.ofSeconds(1)
                                                )
                                                .thenApply(List.class::cast)
                                );
                })
                .flatMap(attachment_list -> {
                    return ServerResponse.ok().body(BodyInserters.fromObject(attachment_list));
                })
                .doOnSuccess(unused -> {
                    this_context.actor_mailbox_ref.tell(public_message_define.SAVE_SIGNAL,ActorRef.noSender());
                });
    }
}
