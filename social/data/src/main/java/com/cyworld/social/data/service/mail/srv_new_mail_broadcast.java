package com.cyworld.social.data.service.mail;

import akka.actor.ActorRef;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.cyworld.social.data.actors.common.actor_entity;
import com.cyworld.social.data.actors.public_message_define;
import com.cyworld.social.data.entity.mail.mailgroup;
import com.cyworld.social.data.service.transaction.srv_helper;
import com.cyworld.social.data.utils.Mail_config_listener;
import com.cyworld.social.utils.request.mail.req_new_broadcast_mail;
import com.cyworld.social.data.actors.mail.actor_mailgroup;
import com.cyworld.social.data.actors.common.actor_transaction;
import com.cyworld.social.data.actors.akka_system;
import com.cyworld.social.data.entity.mail.mail_attachment;
import com.cyworld.social.data.entity.mail.mail_by_broadcast;
import com.cyworld.social.data.utils.static_define;
import com.udojava.evalex.Expression;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.Arrays;

import static akka.pattern.Patterns.ask;

@Component
public class srv_new_mail_broadcast {

    public static class context {
        req_new_broadcast_mail req;
        Mail_config_listener.config config;
        ActorRef actor_mailgroup_ref = null;
        ActorRef actor_mailgroup_child_transaction_ref = null;
        String tx_token;
        String new_mail_id;
    }

    @Autowired
    akka_system akka;

    public Mono serv(ServerRequest request) {
        context this_context = new context();
        this_context.tx_token = srv_helper.get_transaction_token(request);
        this_context.config=Mail_config_listener.ar.get();
        boolean in_transaction = !this_context.tx_token.equals(static_define.no_in_transaction);
        return request.bodyToMono(req_new_broadcast_mail.class)
                .doOnNext(body -> this_context.req = body)
                .flatMap(body -> {
                    try {
                        if(body.getCondition().length()!=0){
                            if (!new Expression(body.getCondition()).isBoolean())
                                return Mono.error(new Throwable("condition not a bool expression"));
                        }
                        return actor_mailgroup.find_or_build
                                (
                                        body.getId_group(),
                                        akka.actor_system
                                );
                    } catch (Exception e) {
                        return Mono.error(e);
                    }
                })
                .doOnNext(ref -> this_context.actor_mailgroup_ref = ref)
                .flatMap(ref -> {
                    if (ref.isTerminated())
                        return Mono.error(new Throwable("cant create actor path=".concat(ref.path().name())));
                    if (!in_transaction)
                        return Mono.just(ref);
                    else
                        return actor_entity.join_transaction(ref,this_context.tx_token);
                })
                .doOnNext(ref -> {
                    if (in_transaction)
                        this_context.actor_mailgroup_child_transaction_ref = ref;
                })
                .flatMap(ref -> {
                    return Mono.fromCompletionStage
                            (
                                    ask
                                            (
                                                    ref,
                                                    ((actor_mailgroup.msg_readonly_inject_ask<mailgroup,String>) grp -> {
                                                        grp.clear_old(this_context.config);
                                                        int mail_id = 1 + Integer.valueOf
                                                                (
                                                                        grp.getInbox()
                                                                                .stream()
                                                                                .map(m -> Integer.valueOf(m.getId()))
                                                                                .max(Integer::compareTo)
                                                                                .orElse(0)
                                                                );
                                                        mail_by_broadcast mail = mail_by_broadcast
                                                                .builder()
                                                                .id(String.valueOf(mail_id))
                                                                .condition(this_context.req.getCondition())
                                                                .content(this_context.req.getContent())
                                                                .title(this_context.req.getTitle())
                                                                .from(this_context.req.getFrom())
                                                                .type(this_context.req.getType())
                                                                .id_group(this_context.req.getId_group())
                                                                .build();
                                                        if (this_context.req.getAttechmentJson() != null)
                                                            Arrays.stream(this_context.req.getAttechmentJson())
                                                                    .map(s -> {
                                                                        JSONObject o = JSON.parseObject(s);
                                                                        return mail_attachment
                                                                                .builder()
                                                                                .id(o.getString("id"))
                                                                                .content(o.getString("content"))
                                                                                .build();
                                                                    })
                                                                    .forEach(mail.getAttachment()::add);
                                                        grp.getInbox().add(mail);
                                                        return String.valueOf(mail_id);
                                                    }),
                                                    Duration.ofSeconds(1)
                                            )
                                            .thenApply(String.class::cast)
                            );
                })
                .doOnNext(mail_id -> this_context.new_mail_id = mail_id)
                .flatMap(mail_id -> {
                    return ServerResponse
                            .ok()
                            .body(BodyInserters.fromObject(mail_id));
                })
                .doOnError(t -> {
                    if (this_context.actor_mailgroup_child_transaction_ref != null)
                        this_context.actor_mailgroup_child_transaction_ref.tell
                                (
                                        ((actor_transaction.msg_undo) () -> {
                                            return this_context.tx_token;
                                        }),
                                        ActorRef.noSender()
                                );
                })
                .doOnSuccess(t -> {
                    if (!in_transaction) {
                        this_context.actor_mailgroup_ref.tell(
                                public_message_define.SAVE_SIGNAL,
                                ActorRef.noSender()
                        );
                    }
                    else {
                        actor_transaction.simple_msg_push_tx_context_teller(
                                this_context.actor_mailgroup_child_transaction_ref.path().toString(),
                                this_context.tx_token,
                                akka.actor_system
                        );
                    }
                });
    }

}
