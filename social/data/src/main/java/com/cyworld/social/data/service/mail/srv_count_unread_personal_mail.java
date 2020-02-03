package com.cyworld.social.data.service.mail;

import akka.actor.ActorRef;
import com.alibaba.fastjson.JSON;
import com.cyworld.social.data.actors.akka_system;
import com.cyworld.social.data.actors.common.actor_entity;
import com.cyworld.social.data.actors.mail.actor_mailbox;
import com.cyworld.social.data.entity.mail.mail_by_personal;
import com.cyworld.social.data.entity.mail.mailbox;
import com.cyworld.social.utils.request.mail.req_count_unread_personal_mail;
import com.cyworld.social.utils.response.mail.mail_type_count;
import com.cyworld.social.utils.response.mail.res_count_unread_personal_mail;
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

import static akka.pattern.Patterns.ask;
@Component
public class srv_count_unread_personal_mail {
    @Autowired
    akka_system akka;
    @Data
    public static class context {
        req_count_unread_personal_mail req;
        ActorRef actor_mailbox_ref;
        res_count_unread_personal_mail res;
    }
    public Mono serv(ServerRequest request) {
        context this_context = new context();

        return request.bodyToMono(req_count_unread_personal_mail.class)
                .flatMap(body -> {
                    this_context.req = body;
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
                                                        ((actor_mailbox.msg_readonly_inject_ask<mailbox,res_count_unread_personal_mail>) box -> {
                                                            res_count_unread_personal_mail res=new res_count_unread_personal_mail();
                                                            res.setDatas(new ArrayList<>());
                                                            for (mail_by_personal mail :
                                                                    box.getInbox()) {
                                                                if(mail.is_read())
                                                                    continue;
                                                                boolean find=false;
                                                                for (mail_type_count mtc:res.getDatas()){
                                                                    if(mtc.getType().equals(mail.getType())){
                                                                        find=true;
                                                                        mtc.setCount(mtc.getCount()+1);
                                                                        break;
                                                                    }
                                                                }
                                                                if(!find){
                                                                    mail_type_count new_mtc=new mail_type_count();
                                                                    new_mtc.setType(mail.getType());
                                                                    new_mtc.setCount(1);
                                                                    res.getDatas().add(new_mtc);
                                                                }
                                                            }
                                                            //res.setCount(box.getInbox().stream().filter(e->!e.is_read()).count());
                                                            return res;
                                                        }),
                                                        Duration.ofSeconds(1)
                                                )
                                                .thenApply(res_count_unread_personal_mail.class::cast)
                                );
                })
                .flatMap(res -> {
                    String s= JSON.toJSONString(res);
                    this_context.res = res;
                    return ServerResponse.ok()
                            .contentType(MediaType.APPLICATION_JSON)
                            .body(BodyInserters.fromObject(s));
                });
    }
}
