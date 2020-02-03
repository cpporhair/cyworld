package com.cyworld.social.data.service.guild;

import akka.actor.ActorRef;
import com.cyworld.social.data.actors.akka_system;
import com.cyworld.social.data.actors.common.actor_entity;
import com.cyworld.social.data.actors.common.actor_transaction;
import com.cyworld.social.data.actors.guild.actor_cached_guild_list;
import com.cyworld.social.data.actors.guild.actor_guild;
import com.cyworld.social.data.actors.guild.actor_player_guild_data;
import com.cyworld.social.data.actors.public_message_define;
import com.cyworld.social.data.entity.guild.guild;
import com.cyworld.social.data.entity.guild.player_guild_data;
import com.cyworld.social.data.mongodb.db_context;
import com.cyworld.social.data.service.transaction.srv_helper;
import com.cyworld.social.data.utils.static_define;
import com.cyworld.social.utils.request.guild.req_new_guild;
import com.cyworld.social.utils.response.guild.res_ask_join_guild;
import com.cyworld.social.utils.response.guild.res_new_guild;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

import static akka.pattern.Patterns.ask;
@Component
public class srv_new_guild {
    private static final Logger logger = LoggerFactory.getLogger(srv_new_guild.class.getName());
    @Autowired
    akka_system akka;
    @Autowired
    db_context mongo_context;

    private class context{
        String tx_token;
        req_new_guild req;
        guild guild;
        ActorRef guild_ref =null;
        ActorRef guild_transaction_ref =null;
        ActorRef owner_ref =null;
        ActorRef owner_transaction_ref =null;
        boolean in_transaction=false;
    }
    public Mono serv(ServerRequest request){
        context this_context = new context();
        this_context.tx_token = srv_helper.get_transaction_token(request);
        this_context.in_transaction = !this_context.tx_token.equals(static_define.no_in_transaction);
        return request.bodyToMono(req_new_guild.class)
                .flatMap(req->{
                    if(req.getName().getBytes().length>18)
                        return Mono.error(new Throwable("name too long"));
                    else
                        return Mono.just(req);
                })
                .doOnNext(req->{
                    this_context.req=req;
                    this_context.guild=guild.from_request(req);
                })
                .flatMap(req->{
                    return db_context.mongo_templates.get("guild").getTemplate()
                            .exists(Query.query(Criteria.where("_id").is(this_context.guild.id)),"guilds")
                            .flatMap(b->{
                                if(b)
                                    return Mono.error(new Throwable("has same guild:"));
                                else
                                    return Mono.just(req);
                            });
                })
                .flatMap(req->{
                    return actor_guild.has_actor(this_context.guild.id, akka.actor_system)
                            .flatMap(b->{
                                if(b)
                                    return Mono.error(new Throwable("has same guild:"));
                                else
                                    return Mono.just(req);
                            });
                })
                .flatMap(req-> {
                    return actor_player_guild_data.find_or_load
                            (
                                    this_context.req.getPlayer_namespace(),
                                    this_context.req.getPlayer_id(),
                                    akka.actor_system
                            )
                            .switchIfEmpty
                                    (
                                            actor_player_guild_data.create
                                                    (
                                                            player_guild_data
                                                                    .builder()
                                                                    .guild_id("NULL")
                                                                    .id(this_context.req.getPlayer_id())
                                                                    .namespace(this_context.req.getPlayer_namespace())
                                                                    .build(),
                                                            akka.actor_system
                                                    )
                                                    .doOnNext(ref->{
                                                        ref.tell(public_message_define.SAVE_SIGNAL,ActorRef.noSender());
                                                    })
                                    )
                            .flatMap(ref -> {
                                return Mono.fromCompletionStage
                                        (
                                                ask
                                                        (
                                                                ref,
                                                                (actor_player_guild_data.msg_inject_ask<player_guild_data, Boolean>) data -> {
                                                                    if (!data.is_guild_empty())
                                                                        return false;
                                                                    else
                                                                        return true;
                                                                },
                                                                Duration.ofSeconds(1)
                                                        )
                                                        .thenApply(Boolean.class::cast)
                                        )
                                        .flatMap(res -> {
                                            if (!res)
                                                return Mono.error(new Throwable("has joined guild"));
                                            return Mono.just(req);
                                        })
                                        .doOnNext(res -> {
                                            this_context.owner_ref = ref;
                                        });
                            });
                })
                .flatMap(req->{
                    return actor_guild.create
                            (
                                    this_context.guild,
                                    akka.actor_system
                            )
                            .flatMap(ref->{
                                if(ref.isTerminated())
                                    return Mono.error(new Throwable("cant create actor"));
                                return Mono.just(ref);
                            })
                            .doOnSuccess(ref->{
                                this_context.guild_ref =ref;
                            });
                })
                .flatMap(ref->{
                    if(ref.isTerminated())
                        return Mono.error(new Throwable("cant create actor"));
                    if (!this_context.in_transaction)
                        return Mono.just(ref);
                    else
                        return actor_entity.join_transaction(ref,this_context.tx_token)
                                .flatMap(transaction_ref->{
                                    if(transaction_ref.isTerminated())
                                        return Mono.error(new Throwable("cant create actor"));
                                    else
                                        return Mono.just(transaction_ref);
                                })
                                .doOnSuccess(transaction_ref->{
                                    this_context.guild_transaction_ref =transaction_ref;
                                });
                })
                .flatMap(unused->{
                    if (!this_context.in_transaction)
                        return Mono.just(this_context.owner_ref);
                    else
                        return actor_entity.join_transaction(this_context.owner_ref,this_context.tx_token)
                                .doOnNext(transaction_ref->{
                                    this_context.owner_transaction_ref =transaction_ref;
                                });
                })
                .doOnNext(ref->{
                    ref.tell
                            (
                                    (actor_player_guild_data.msg_inject_tell<player_guild_data>)data->{
                                        data.setGuild_id(this_context.guild.id);
                                    }
                                    ,ActorRef.noSender()
                            );
                })
                .doOnNext(ref->{
                    if (!this_context.in_transaction) {
                        this_context.guild_ref
                                .tell(public_message_define.SAVE_SIGNAL,
                                        ActorRef.noSender());
                        this_context.owner_ref
                                .tell(public_message_define.SAVE_SIGNAL,
                                        ActorRef.noSender());
                        actor_cached_guild_list.find_or_build(akka.actor_system)
                                .doOnNext(ref_cached_guild_list->{
                                    ref_cached_guild_list.tell(
                                            actor_cached_guild_list.update_one
                                                    .builder()
                                                    .guild_id(this_context.guild.id)
                                                    .build(),
                                            ActorRef.noSender()
                                    );
                                })
                                .subscribe();
                    }
                    else{
                        if (this_context.guild_transaction_ref != null) {
                            actor_transaction.simple_msg_push_tx_context_teller(
                                    this_context.guild_transaction_ref.path().toString(),
                                    this_context.tx_token,
                                    akka.actor_system
                            );
                            actor_cached_guild_list.find_or_build(akka.actor_system)
                                    .doOnNext(ref_cached_guild_list->{
                                        ref_cached_guild_list.tell(
                                                actor_cached_guild_list.update_one
                                                        .builder()
                                                        .guild_id(this_context.guild.id)
                                                        .build(),
                                                ActorRef.noSender()
                                        );
                                    })
                                    .subscribe();
                        }
                        if (this_context.owner_transaction_ref != null) {
                            actor_transaction.simple_msg_push_tx_context_teller(
                                    this_context.owner_transaction_ref.path().toString(),
                                    this_context.tx_token,
                                    akka.actor_system
                            );
                        }
                    }
                })
                .doOnError(e->{
                    if (this_context.guild_transaction_ref != null) {
                        this_context.guild_transaction_ref.tell
                                (
                                        ((actor_transaction.msg_undo) () -> {
                                            return this_context.tx_token;
                                        }),
                                        ActorRef.noSender()
                                );
                    }
                    if (this_context.owner_transaction_ref != null) {
                        this_context.owner_transaction_ref.tell
                                (
                                        ((actor_transaction.msg_undo) () -> {
                                            return this_context.tx_token;
                                        }),
                                        ActorRef.noSender()
                                );
                    }
                })
                .flatMap(res->{
                    res_new_guild resNewGuild=new res_new_guild();
                    resNewGuild.setGuild_id(this_context.guild.id);
                    return ServerResponse.ok()
                            .contentType(MediaType.APPLICATION_JSON)
                            .body(BodyInserters.fromObject(resNewGuild));
                });

    }
}
