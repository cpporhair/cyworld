package com.cyworld.social.data.service.guild;

import akka.actor.ActorRef;
import com.cyworld.social.data.actors.akka_system;
import com.cyworld.social.data.actors.common.actor_entity;
import com.cyworld.social.data.actors.common.actor_transaction;
import com.cyworld.social.data.actors.guild.actor_guild;
import com.cyworld.social.data.actors.guild.actor_player_guild_data;
import com.cyworld.social.data.actors.public_message_define;
import com.cyworld.social.data.entity.guild.guild;
import com.cyworld.social.data.entity.guild.player_guild_data;
import com.cyworld.social.data.service.transaction.srv_helper;
import com.cyworld.social.data.utils.static_define;
import com.cyworld.social.utils.request.guild.req_join;
import com.cyworld.social.utils.response.common.throwable_res;
import com.cyworld.social.utils.response.guild.res_join;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

import java.time.Duration;

import static akka.pattern.Patterns.ask;
@Component
public class srv_join {
    private static final Logger logger = LoggerFactory.getLogger(srv_join.class.getName());
    @Autowired
    akka_system akka;

    private static class context{
        req_join req;
        String tx_token;
        ActorRef guild_ref =null;
        ActorRef player_ref=null;
    }

    public Mono serv(ServerRequest request){
        context this_context = new context();
        this_context.tx_token = srv_helper.get_transaction_token(request);
        boolean in_transaction = !this_context.tx_token.equals(static_define.no_in_transaction);
        return request.bodyToMono(req_join.class)
                .doOnNext(req->{
                    this_context.req=req;
                })
                .flatMap(req->{
                    return actor_player_guild_data.find_or_load(req.getPlayer_namespace(),req.getPlayer_id(),akka.actor_system)
                            .switchIfEmpty
                                    (
                                            actor_player_guild_data.create
                                                    (
                                                            player_guild_data
                                                                    .builder()
                                                                    .guild_id("NULL")
                                                                    .id(req.getPlayer_id())
                                                                    .namespace(req.getPlayer_namespace())
                                                                    .build(),
                                                            akka.actor_system
                                                    )
                                                    .doOnNext(ref->{
                                                        ref.tell(public_message_define.SAVE_SIGNAL,ActorRef.noSender());
                                                    })
                                    );
                })
                .flatMap(player_ref->{
                    if(player_ref.isTerminated())
                        return Mono.error(new Throwable("cant create player actorref"));
                    this_context.player_ref=player_ref;
                    return actor_guild.find_or_load(this_context.req.getGuild_id(),akka.actor_system);
                })
                .flatMap(guild_ref->{
                    if(guild_ref.isTerminated())
                        return Mono.error(new Throwable("cant find guild actorref"));
                    this_context.guild_ref=guild_ref;
                    return Mono.just(guild_ref);
                })
                .flatMap(unused->{
                    if(in_transaction){
                        return Mono.just(1)
                                .flatMap(u->{
                                    return actor_entity.join_transaction(this_context.player_ref,this_context.tx_token)
                                            .doOnNext(ref->{
                                                this_context.player_ref=ref;
                                            });
                                })
                                .flatMap(u->{
                                    return actor_entity.join_transaction(this_context.guild_ref,this_context.tx_token)
                                            .doOnNext(ref->{
                                                this_context.guild_ref=ref;
                                            });
                                })
                                .map(u->1);

                    }
                    else{
                        return Mono.just(1);
                    }
                })
                .flatMap(u->{
                    return Mono.fromCompletionStage
                            (
                                    ask
                                            (
                                                    this_context.player_ref,
                                                    (actor_player_guild_data.msg_inject_ask<player_guild_data,Integer>)data->{
                                                        if(!data.is_guild_empty())
                                                            return res_join.result_has_joined;
                                                        return res_join.result_ok;
                                                    },
                                                    Duration.ofSeconds(1)
                                            )
                                            .thenApply(Integer.class::cast)
                            );
                })
                .flatMap(res->{
                    switch (res){
                        case res_join.result_ok:
                            return Mono.just(this_context.req);
                        default:
                            return Mono.error(new throwable_res(res.toString()));
                    }
                })
                .flatMap(ref->{
                    return Mono.fromCompletionStage
                            (
                                    ask
                                            (
                                                    this_context.guild_ref,
                                                    (actor_guild.msg_inject_ask<guild, Integer>) data->{
                                                        return data.join(this_context.req);
                                                    },
                                                    Duration.ofSeconds(1)
                                            )
                                            .thenApply(o->Integer.class.cast(o))
                            );
                })
                .flatMap(res->{
                    switch (res){
                        case res_join.result_ok:
                            this_context.player_ref.tell(
                                    (actor_player_guild_data.msg_inject_tell<player_guild_data>)data->{
                                        data.setGuild_id(this_context.req.getGuild_id());
                                    },
                                    ActorRef.noSender()
                            );
                            return Mono.just(res);
                        default:
                            return Mono.error(new throwable_res(res.toString()));
                    }
                })
                .doOnError(i->{
                    if(in_transaction){
                        this_context.player_ref.tell
                                (
                                        ((actor_transaction.msg_undo) () -> {
                                            return this_context.tx_token;
                                        }),
                                        ActorRef.noSender()
                                );
                        this_context.guild_ref.tell
                                (
                                        ((actor_transaction.msg_undo) () -> {
                                            return this_context.tx_token;
                                        }),
                                        ActorRef.noSender()
                                );
                    }
                })
                .doOnNext(i->{
                    if(!in_transaction){
                        this_context.player_ref.tell(public_message_define.SAVE_SIGNAL,ActorRef.noSender());
                        this_context.guild_ref.tell(public_message_define.SAVE_SIGNAL,ActorRef.noSender());
                    }
                    else {
                        actor_transaction.simple_msg_push_tx_context_teller(
                                this_context.guild_ref.path().toString(),this_context.tx_token, akka.actor_system);
                        actor_transaction.simple_msg_push_tx_context_teller(
                                this_context.player_ref.path().toString(),this_context.tx_token, akka.actor_system);
                    }
                })
                .onErrorResume(e->{
                    if(e instanceof throwable_res)
                        return Mono.just(Integer.valueOf(e.getMessage()));
                    else
                        return Mono.error(e);
                })
                .flatMap(res->{
                    switch (res){
                        case res_join.result_ok:
                            return ServerResponse.ok()
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .body(BodyInserters.fromObject(
                                            new res_join(res)));
                        default:
                            return ServerResponse.ok()
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .body(BodyInserters.fromObject(new res_join(res)));
                    }
                });

    }
}
