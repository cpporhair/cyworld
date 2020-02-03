package com.cyworld.social.data.service.mail;

import akka.actor.ActorRef;
import com.cyworld.social.data.actors.common.actor_entity;
import com.cyworld.social.data.service.transaction.srv_helper;
import com.cyworld.social.utils.request.global.req_new_server;
import com.cyworld.social.data.actors.mail.actor_mailgroup;
import com.cyworld.social.data.actors.common.actor_transaction;
import com.cyworld.social.data.actors.akka_system;
import com.cyworld.social.data.actors.public_message_define;
import com.cyworld.social.data.entity.mail.mailgroup;
import com.cyworld.social.data.mongodb.db_context;
import com.cyworld.social.data.utils.static_define;
import lombok.Builder;
import lombok.Data;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;

import static akka.pattern.Patterns.ask;

import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.TreeSet;

@Component
public class srv_new_mailgroup {
    @Data
    @Builder
    private static class context {
        req_new_server req;
        ActorRef mailgroup_actor_ref;
        String tx_token;
        ActorRef child_transaction_actor_ref;
        ReactiveMongoTemplate mongo;
    }

    @Autowired
    akka_system akka;
    @Autowired
    db_context mongo_context;

    public Mono serv(ServerRequest request) {
        context this_context = context.builder()
                .tx_token(srv_helper.get_transaction_token(request))
                .build();
        boolean in_transaction = !this_context.tx_token.equals(static_define.no_in_transaction);
        return request.bodyToMono(req_new_server.class)
                .flatMap(req -> {
                    this_context.req = req;
                    String actor_path = actor_mailgroup.compute_search_path(this_context.req.id);
                    if (!akka.actor_system.actorFor(actor_path).isTerminated())
                        return Mono.error(new Throwable("has same mailgroup:".concat(actor_path)));
                    this_context.mongo = mongo_context.mongo_templates.get("mail").getTemplate();
                    if (null == this_context.mongo)
                        return Mono.error(new Throwable("cant find db mail"));
                    return this_context.mongo.exists(Query.query(Criteria.where("_id").is(req.id)), mailgroup.class, "mailgroup");
                })
                .flatMap(exists -> {
                    if (exists)
                        return Mono.error(new Throwable("has same id"));
                    return actor_mailgroup
                            .create(mailgroup.builder()
                                            .id(this_context.req.id)
                                            .build(),
                                    akka.actor_system);
                })
                .doOnNext(ref -> {
                    this_context.mailgroup_actor_ref = ref;
                })
                .flatMap(ref -> {
                    if (ref.isTerminated())
                        return Mono.error(new Throwable("cant create actor"));
                    if (!in_transaction)
                        return Mono.just(ref);
                    else
                        return actor_entity.join_transaction(ref,this_context.tx_token);
                })
                .doOnNext(ref -> {
                    if (in_transaction)
                        this_context.child_transaction_actor_ref = ref;
                })
                .flatMap(ref -> {
                    return Mono.fromCompletionStage(
                            ask(
                                    ref,
                                    ((actor_mailgroup.msg_inject_ask<mailgroup,Boolean>) grp -> {
                                        grp.setDescription(this_context.req.description);
                                        grp.setId(this_context.req.id);
                                        grp.setInbox(new TreeSet<>());
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
                .doOnError(notused -> {
                    if (this_context.child_transaction_actor_ref != null) {
                        this_context.child_transaction_actor_ref.tell
                                (
                                        ((actor_transaction.msg_undo) () -> {
                                            return this_context.tx_token;
                                        }),
                                        ActorRef.noSender()
                                );
                    }
                    if (this_context.mailgroup_actor_ref != null) {
                        this_context.mailgroup_actor_ref.tell(public_message_define.CLOSE_SIGNAL, ActorRef.noSender());
                    }
                })
                .doOnSuccess(notused -> {
                    if (!in_transaction)
                        this_context.mailgroup_actor_ref.tell
                                (
                                        public_message_define.SAVE_SIGNAL,
                                        ActorRef.noSender()
                                );
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
