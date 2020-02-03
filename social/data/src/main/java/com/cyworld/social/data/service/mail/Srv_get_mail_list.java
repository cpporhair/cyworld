package com.cyworld.social.data.service.mail;

import akka.actor.ActorRef;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.cyworld.social.data.entity.mail.mailbox;
import com.cyworld.social.data.utils.Mail_config_listener;
import com.cyworld.social.utils.request.mail.req_get_mail_list;
import com.cyworld.social.data.actors.mail.actor_mailbox;
import com.cyworld.social.data.actors.akka_system;
import com.cyworld.social.data.mongodb.db_context;
import com.cyworld.social.utils.response.mail.mail_list_info;
import com.cyworld.social.utils.response.mail.res_get_mail_list;
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
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static akka.pattern.Patterns.ask;

@Component
public class Srv_get_mail_list {
    @Autowired
    akka_system akka;
    @Data
    public static class context {
        req_get_mail_list req;
        ActorRef actor_mailbox_ref;
        res_get_mail_list res;
        Mail_config_listener.config config;
    }

    public Mono serv(ServerRequest request) {
        context this_context = new context();

        return request.bodyToMono(req_get_mail_list.class)
                .flatMap(body -> {
                    this_context.req = body;
                    this_context.config=Mail_config_listener.ar.get();
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
                                                        ((actor_mailbox.msg_readonly_inject_ask<mailbox,res_get_mail_list>) box -> {
                                                            res_get_mail_list res=new res_get_mail_list();
                                                            res.setMail_keep_timer_in_config(this_context.config.getMail_personal_keep_millis());
                                                            box.clear_old(this_context.config);

                                                            res.setMax_size(box.getInbox()
                                                                    .stream()
                                                                    .filter(mail->mail.getType().equals(this_context.req.getType()))
                                                                    .count());
                                                            List l=box
                                                                    .getInbox()
                                                                    .stream()
                                                                    .filter(mail->mail.getType().equals(this_context.req.getType()))
                                                                    .skip(Long.valueOf(this_context.req.getSkip()))
                                                                    .limit(Long.valueOf(this_context.req.getTake()))
                                                                    .map(mail->{
                                                                        mail_list_info info = new mail_list_info();
                                                                        info.setId(mail.getId());
                                                                        info.setId(mail.getId());
                                                                        info.setType(mail.getType());
                                                                        info.setFrom(mail.getFrom());
                                                                        info.setRead(String.valueOf(mail.is_read()));
                                                                        info.setTime_stamp(String.valueOf(mail.getTime_stamp()));
                                                                        info.setTitle(mail.getTitle());
                                                                        if(mail.getAttachment()==null)
                                                                            info.setUnread_attachment(false);
                                                                        else
                                                                            info.setUnread_attachment(mail.getAttachment().stream().anyMatch(o->!o.isRead()));
                                                                        return info;
                                                                    })
                                                                    .collect(Collectors.toList());
                                                            res.setMails(l);
                                                            return res;
                                                        }),
                                                        Duration.ofSeconds(1)
                                                )
                                                .thenApply(res_get_mail_list.class::cast)
                                );
                })
                .flatMap(res -> {
                    String s=JSON.toJSONString(res);
                    this_context.res = res;
                    return ServerResponse.ok()
                            .contentType(MediaType.APPLICATION_JSON)
                            .body(BodyInserters.fromObject(s));
                });
    }
}
