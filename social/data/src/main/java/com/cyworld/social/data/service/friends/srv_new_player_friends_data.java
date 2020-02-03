package com.cyworld.social.data.service.friends;

import akka.actor.ActorRef;
import com.cyworld.social.data.actors.akka_system;
import com.cyworld.social.data.actors.common.actor_transaction;
import com.cyworld.social.data.actors.friends.actor_friends_data;
import com.cyworld.social.data.actors.public_message_define;
import com.cyworld.social.data.entity.friends.friend;
import com.cyworld.social.data.entity.friends.player_friends_data;
import com.cyworld.social.data.mongodb.db_context;
import com.cyworld.social.data.service.transaction.srv_helper;
import com.cyworld.social.data.utils.static_define;
import com.cyworld.social.utils.request.global.req_new_user;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
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
public class srv_new_player_friends_data {
    private static class context {
        String tx_token;
        ActorRef player_friends_data_actor_ref;
        ActorRef child_transaction_actor_ref;
        req_new_user req;
        ReactiveMongoTemplate mongo;
    }

    @Autowired
    akka_system akka;
    @Autowired
    db_context mongo_context;

    public Mono serv(ServerRequest request){
        context this_context = new context();
        this_context.tx_token = srv_helper.get_transaction_token(request);
        boolean in_transaction = !this_context.tx_token.equals(static_define.no_in_transaction);
        boolean actor_created = false;

        return request.bodyToMono(req_new_user.class)
                .flatMap(req->{
                    this_context.req=req;
                    String actor_path = actor_friends_data.compute_search_path(req.getNamespace(), req.getPlayer_id());
                    this_context.mongo = mongo_context.mongo_templates.get("friend").getTemplate();
                    if (null == this_context.mongo)
                        return Mono.error(new Throwable("cant find db friend"));
                    if (!akka.actor_system.actorFor(actor_path).isTerminated())
                        return Mono.error(new Throwable("has same player:".concat(actor_path)));
                    return this_context.mongo.exists(Query.query(Criteria.where("_id").is(req.getPlayer_id())), req.getNamespace());
                })
                .flatMap(has->{
                    if(has)
                        return Mono.error(new Throwable("has same id"));
                    else
                        return actor_friends_data.create(
                                player_friends_data.builder()
                                        ._id(this_context.req.getPlayer_id())
                                        .namespace(this_context.req.getNamespace())
                                .build()
                                ,akka.actor_system
                        );
                })
                .doOnNext(ref->{
                    this_context.player_friends_data_actor_ref=ref;
                })
                .flatMap(ref->{
                    if(ref.isTerminated())
                        return Mono.error(new Throwable("cant create actor"));
                    if (!in_transaction)
                        return Mono.just(ref);
                    else
                        return actor_friends_data.join_transaction(ref,this_context.tx_token);
                })
                .doOnNext(tx_ref -> {
                    if(!tx_ref.equals(this_context.player_friends_data_actor_ref))
                        this_context.child_transaction_actor_ref = tx_ref;
                    else
                        this_context.child_transaction_actor_ref=null;
                })
                .flatMap(ref->{
                    return Mono.fromCompletionStage(
                            ask(
                                    ref,
                                    ((actor_friends_data.msg_inject_ask<player_friends_data,Boolean>) data -> {
                                        data.setFriends(new ArrayList<friend>());
                                        data.setNamespace(this_context.req.getNamespace());
                                        return Boolean.TRUE;
                                    }),
                                    Duration.ofSeconds(1)
                            ).thenApply(Boolean.class::cast)
                    );
                })
                .flatMap(res->{
                    return ServerResponse.ok().contentType(MediaType.APPLICATION_JSON).body(BodyInserters.fromObject(this_context.req));
                })
                .doFinally(o->{
                    switch (o){
                        case ON_ERROR:
                        case CANCEL:
                            if (this_context.child_transaction_actor_ref != null) {
                                this_context.child_transaction_actor_ref.tell
                                        (
                                                ((actor_transaction.msg_undo) () -> {
                                                    return this_context.tx_token;
                                                }),
                                                ActorRef.noSender()
                                        );
                            }
                            if (this_context.player_friends_data_actor_ref != null) {
                                this_context.player_friends_data_actor_ref.tell(public_message_define.CLOSE_SIGNAL, ActorRef.noSender());
                            }
                            return;
                        case ON_COMPLETE:
                            if (!in_transaction) {
                                this_context.player_friends_data_actor_ref.tell(
                                        public_message_define.SAVE_SIGNAL,
                                        ActorRef.noSender()
                                );
                            }
                            else{
                                actor_transaction.simple_msg_push_tx_context_teller(
                                        this_context.child_transaction_actor_ref.path().toString(),
                                        this_context.tx_token,
                                        akka.actor_system
                                );
                            }
                            return;
                    }
                });
    }
}
