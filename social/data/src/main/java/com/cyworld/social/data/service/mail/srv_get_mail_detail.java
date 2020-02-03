package com.cyworld.social.data.service.mail;

import akka.actor.ActorRef;
import com.alibaba.fastjson.JSON;
import com.cyworld.social.data.actors.mail.actor_mailbox;
import com.cyworld.social.data.actors.akka_system;
import com.cyworld.social.data.actors.public_message_define;
import com.cyworld.social.data.entity.mail.mail_by_personal;
import com.cyworld.social.data.entity.mail.mailbox;
import com.cyworld.social.data.mongodb.db_context;
import com.cyworld.social.utils.request.mail.req_get_mail_detail;
import com.cyworld.social.utils.response.mail.res_get_mail_detail;
import lombok.Data;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.stream.Collectors;

import static akka.pattern.Patterns.ask;

@Component
public class srv_get_mail_detail {

    @Autowired
    akka_system akka;

    @Data
    public static class context {
        req_get_mail_detail req;
        ActorRef actor_mailbox_ref;
    }

    public Mono serv(ServerRequest request) {
        context this_context = new context();
        return request.bodyToMono(req_get_mail_detail.class)
                .doOnNext(this_context::setReq)
                .flatMap(body -> {
                    return actor_mailbox.find_or_build(
                            body.getNamesapce(),
                            body.getId(),
                            akka.actor_system
                    );
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
                                                        ((actor_mailbox.msg_inject_ask<mailbox,res_get_mail_detail>) box -> {
                                                            for (mail_by_personal m : box.getInbox()) {
                                                                if (!m.getId().equals(this_context.req.getMail_id()))
                                                                    continue;
                                                                m.set_read(true);
                                                                res_get_mail_detail res = new res_get_mail_detail();
                                                                res.setId(m.getId());
                                                                res.setContent(m.getContent());
                                                                res.setFrom(m.getFrom());
                                                                res.setType(m.getType());
                                                                res.setRead(String.valueOf(m.is_read()));
                                                                res.setTime_stamp(String.valueOf(m.getTime_stamp()));
                                                                res.setTitle(m.getTitle());
                                                                res.setAttachmentJson(
                                                                        m.getAttachment()
                                                                                .stream()
                                                                                .map(a -> JSON.toJSONString(a))
                                                                                .collect(Collectors.toList())
                                                                );
                                                                return res;
                                                            }
                                                            return new res_get_mail_detail();
                                                        }),
                                                        Duration.ofSeconds(1)
                                                )
                                                .thenApply(res_get_mail_detail.class::cast)
                                );
                })
                .flatMap(m -> {
                    if (m.getId() == null) {
                        return Mono.error(new Throwable("cant find mail id=".concat(this_context.req.getMail_id())));
                    } else {
                        return ServerResponse.ok().contentType(MediaType.APPLICATION_JSON).body(BodyInserters.fromObject(m));
                    }
                })
                .doOnSuccess(m -> {
                    this_context.actor_mailbox_ref.tell(public_message_define.SAVE_SIGNAL,ActorRef.noSender());
                });
    }
}
