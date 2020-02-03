package com.cyworld.social.data.service.mail;

import akka.actor.ActorRef;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.cyworld.social.data.actors.common.actor_entity;
import com.cyworld.social.data.actors.public_message_define;
import com.cyworld.social.data.entity.mail.mailbox;
import com.cyworld.social.data.service.transaction.srv_helper;
import com.cyworld.social.utils.request.mail.req_new_personal_mail;
import com.cyworld.social.data.actors.mail.actor_mailbox;
import com.cyworld.social.data.actors.common.actor_transaction;
import com.cyworld.social.data.actors.akka_system;
import com.cyworld.social.data.entity.mail.mail_by_personal;
import com.cyworld.social.data.entity.mail.mail_attachment;
import com.cyworld.social.data.utils.static_define;
import lombok.Builder;
import lombok.Data;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;

import static akka.pattern.Patterns.ask;

import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.Arrays;

@Component
public class Srv_new_mail_personal {

    @Data
    @Builder
    public static class context {
        req_new_personal_mail req;
        String tx_token;
        ActorRef actor_mailbox_ref;
        ActorRef actor_mailbox_child_transaction_ref;
    }

    @Autowired
    akka_system akka;

    public Mono serv(ServerRequest request) {
        context this_context = context
                .builder()
                .tx_token(srv_helper.get_transaction_token(request))
                .build();
        boolean in_transaction = !this_context.tx_token.equals(static_define.no_in_transaction);
        return request.bodyToMono(req_new_personal_mail.class)
                .flatMap(body -> {
                    this_context.req = body;
                    return actor_mailbox.find_or_build
                            (
                                    this_context.req.getId_namespace(),
                                    this_context.req.getId_box(),
                                    akka.actor_system
                            );
                })
                .doOnNext(ref -> this_context.actor_mailbox_ref = ref)
                .flatMap(ref -> {
                    if (in_transaction)
                        return actor_entity.join_transaction(ref,this_context.tx_token)
                                .doOnNext(r -> this_context.actor_mailbox_child_transaction_ref = r);
                    else
                        return Mono.just(ref);

                })
                .flatMap(ref -> {
                    return Mono.fromCompletionStage
                            (
                                    ask
                                            (
                                                    ref,
                                                    ((actor_mailbox.msg_inject_ask<mailbox,String>) box -> {
                                                        int mail_id = 1 + Integer.valueOf
                                                                (
                                                                        box.getInbox()
                                                                                .stream()
                                                                                .map(m -> Integer.valueOf(m.getId()))
                                                                                .max(Integer::compareTo)
                                                                                .orElse(0)
                                                                );
                                                        mail_by_personal new_mailBypersonal = mail_by_personal.builder()
                                                                .id(String.valueOf(mail_id))
                                                                .id_box(this_context.req.getId_box())
                                                                .id_namespace(this_context.req.getId_namespace())
                                                                .content(this_context.req.getContent())
                                                                .type(this_context.req.getType())
                                                                .from(this_context.req.getFrom())
                                                                .title(this_context.req.getTitle())
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
                                                                    .forEach(new_mailBypersonal.getAttachment()::add);
                                                        box.getInbox().add(new_mailBypersonal);
                                                        return String.valueOf(mail_id);
                                                    }),
                                                    Duration.ofSeconds(1)
                                            )
                                            .thenApply(String.class::cast)
                            );
                })
                .flatMap(s -> ServerResponse.ok().body(BodyInserters.fromObject("OK")))
                .doOnError(unused -> {
                    if (this_context.actor_mailbox_child_transaction_ref != null)
                        this_context.actor_mailbox_child_transaction_ref.tell
                                (
                                        ((actor_transaction.msg_undo) () -> {
                                            return this_context.tx_token;
                                        }),
                                        ActorRef.noSender()
                                );
                })
                .doOnSuccess(unused -> {
                    if (!in_transaction) {
                        this_context.actor_mailbox_ref.tell(public_message_define.SAVE_SIGNAL, ActorRef.noSender());
                    } else {
                        actor_transaction.simple_msg_push_tx_context_teller(
                                this_context.actor_mailbox_child_transaction_ref.path().toString(),
                                this_context.tx_token,
                                akka.actor_system
                        );
                    }
                });
    }
}
