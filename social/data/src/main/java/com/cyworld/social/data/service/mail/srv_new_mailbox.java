package com.cyworld.social.data.service.mail;

import akka.actor.ActorRef;
import com.cyworld.social.data.actors.common.actor_entity;
import com.cyworld.social.data.actors.common.actor_transaction;
import com.cyworld.social.data.actors.mail.actor_mailbox;
import com.cyworld.social.data.service.transaction.srv_helper;
import com.cyworld.social.utils.request.global.req_new_user;
import com.cyworld.social.data.actors.*;
import com.cyworld.social.data.entity.mail.mailbox;
import com.cyworld.social.data.mongodb.db_context;
import com.cyworld.social.data.utils.static_define;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

import static akka.pattern.Patterns.ask;

import java.time.Duration;
import java.util.ArrayList;
import java.util.TreeSet;

@Component
public class srv_new_mailbox {

    private static class context {
        String tx_token;
        ActorRef mailbox_actor_ref;
        ActorRef child_transaction_actor_ref;
        req_new_user req;
    }

    @Autowired
    akka_system akka;

    public Mono serv(ServerRequest request) {
        context this_context = new context();
        this_context.tx_token = srv_helper.get_transaction_token(request);
        boolean in_transaction = !this_context.tx_token.equals(static_define.no_in_transaction);
        boolean actor_mailbox_created = false;
        return request.bodyToMono(req_new_user.class)
                .flatMap(req -> {
                    this_context.req = req;
                    String actor_path = actor_mailbox.compute_search_path(req.getNamespace(), req.getPlayer_id());
                    if (!akka.actor_system.actorFor(actor_path).isTerminated())
                        return Mono.error(new Throwable("has same mailbox:".concat(actor_path)));
                    return db_context
                            .mongo_templates.get("mail").getTemplate()
                            .exists(Query.query(Criteria.where("_id").is(req.getPlayer_id())), req.getNamespace());
                })
                .flatMap(exists -> {
                    if (exists)
                        return Mono.error(new Throwable("has same id"));
                    else
                        return actor_mailbox.create
                                (
                                        mailbox
                                                .builder()
                                                ._id(this_context.req.getPlayer_id())
                                                .namespace(this_context.req.getNamespace())
                                                .build(),
                                        akka.actor_system
                                );
                })
                .doOnNext(ref -> {
                    this_context.mailbox_actor_ref = ref;
                })
                .flatMap(ref -> {
                    if (ref.isTerminated())
                        return Mono.error(new Throwable("cant create actor"));
                    if (!in_transaction)
                        return Mono.just(ref);
                    else
                        return actor_entity.join_transaction(ref,this_context.tx_token);
                })
                .doOnNext(tx_ref -> {
                    this_context.child_transaction_actor_ref = tx_ref;
                })
                .flatMap(ref -> {
                    return Mono.fromCompletionStage(
                            ask(
                                    ref,
                                    ((actor_mailbox.msg_inject_ask<mailbox,Boolean>) box -> {
                                        box.setInbox(new TreeSet<>());
                                        box.setMail_groups(new ArrayList<>());
                                        box.setNamespace(this_context.req.getNamespace());
                                        return Boolean.TRUE;
                                    }),
                                    Duration.ofSeconds(1)
                            )
                    );
                })
                .flatMap(unused -> {
                    return ServerResponse
                            .ok()
                            .body(BodyInserters.fromObject("OK"));
                })
                .doOnError(unused -> {
                    if (this_context.child_transaction_actor_ref != null) {
                        this_context.child_transaction_actor_ref.tell
                                (
                                        ((actor_transaction.msg_undo) () -> {
                                            return this_context.tx_token;
                                        }),
                                        ActorRef.noSender()
                                );
                    }
                    if (this_context.mailbox_actor_ref != null) {
                        this_context.mailbox_actor_ref.tell(public_message_define.CLOSE_SIGNAL, ActorRef.noSender());
                    }
                })
                .doOnSuccess(unused -> {
                    if (!in_transaction) {
                        this_context.mailbox_actor_ref
                                .tell(public_message_define.SAVE_SIGNAL,
                                        ActorRef.noSender());
                    }
                    else {
                        actor_transaction.simple_msg_push_tx_context_teller(
                                this_context.child_transaction_actor_ref.path().toString(),
                                this_context.tx_token,
                                akka.actor_system
                        );
                    }
                })
                .log();
    }
}
