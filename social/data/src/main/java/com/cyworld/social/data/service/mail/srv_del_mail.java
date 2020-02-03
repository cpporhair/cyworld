package com.cyworld.social.data.service.mail;

import akka.actor.ActorRef;
import com.alibaba.fastjson.JSON;
import com.cyworld.social.data.actors.akka_system;
import com.cyworld.social.data.actors.common.actor_entity;
import com.cyworld.social.data.actors.mail.actor_mailbox;
import com.cyworld.social.data.actors.public_message_define;
import com.cyworld.social.data.entity.mail.mail_by_personal;
import com.cyworld.social.data.entity.mail.mailbox;
import com.cyworld.social.utils.request.mail.req_del_mail;
import com.cyworld.social.utils.response.mail.res_delete_mail;
import com.cyworld.social.utils.response.mail.res_get_mail_detail;
import lombok.Data;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.ArrayList;
import java.util.stream.Collectors;

import static akka.pattern.Patterns.ask;

@Component
public class srv_del_mail {
    @Autowired
    akka_system akka;

    @Data
    public static class context {
        req_del_mail req;
        ActorRef actor_mailbox_ref;
    }

    public Mono serv(ServerRequest request) {
        context this_context = new context();
        return request.bodyToMono(req_del_mail.class)
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
                                                        ((actor_mailbox.msg_inject_ask<mailbox, res_delete_mail>) box -> {
                                                            res_delete_mail res=new res_delete_mail();
                                                            res.setMails(new ArrayList<>());
                                                            String[] ids=this_context.req.getMail_id_list().split(";");
                                                            box.getInbox().stream()
                                                                    .filter(mail->{
                                                                        for (String del_id:ids) {
                                                                            if(del_id.equals(mail.getId()))
                                                                                return true;
                                                                        }
                                                                        return false;
                                                                    })
                                                                    .forEach(mail->{
                                                                        res_get_mail_detail detail=new res_get_mail_detail();
                                                                        mail.set_read(true);
                                                                        detail.setId(mail.getId());
                                                                        detail.setContent(mail.getContent());
                                                                        detail.setFrom(mail.getFrom());
                                                                        detail.setType(mail.getType());
                                                                        detail.setRead(String.valueOf(mail.is_read()));
                                                                        detail.setTime_stamp(String.valueOf(mail.getTime_stamp()));
                                                                        detail.setTitle(mail.getTitle());
                                                                        detail.setAttachmentJson(
                                                                                mail.getAttachment()
                                                                                        .stream()
                                                                                        .filter(a->!a.isRead())
                                                                                        .map(a -> JSON.toJSONString(a))
                                                                                        .collect(Collectors.toList())
                                                                        );
                                                                        res.getMails().add(detail);
                                                                    });
                                                            box.getInbox().removeIf(mail->{
                                                                for (String del_id:ids) {
                                                                    if(del_id.equals(mail.getId()))
                                                                        return true;
                                                                }
                                                                return false;
                                                            });
                                                            return res;
                                                        }),
                                                        Duration.ofSeconds(1)
                                                )
                                                .thenApply(res_delete_mail.class::cast)
                                );
                })
                .flatMap(m -> {
                    this_context.actor_mailbox_ref.tell(public_message_define.SAVE_SIGNAL,ActorRef.noSender());
                    return ServerResponse.ok().contentType(MediaType.APPLICATION_JSON).body(BodyInserters.fromObject(m));
                });
    }
}
