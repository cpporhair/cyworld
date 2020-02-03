package com.cyworld.social.data.service.guild;

import akka.actor.ActorRef;
import com.cyworld.social.data.actors.akka_system;
import com.cyworld.social.data.actors.common.actor_entity;
import com.cyworld.social.data.actors.common.actor_transaction;
import com.cyworld.social.data.actors.guild.actor_guild;
import com.cyworld.social.data.actors.guild.actor_player_guild_data;
import com.cyworld.social.data.actors.public_message_define;
import com.cyworld.social.data.entity.guild.*;
import com.cyworld.social.data.service.transaction.srv_helper;
import com.cyworld.social.data.utils.static_define;
import com.cyworld.social.utils.request.guild.req_apply_join_guild;
import com.cyworld.social.utils.response.common.throwable_res;
import com.cyworld.social.utils.response.guild.res_apply_join;
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
public class srv_apply_join {
    private static final Logger logger = LoggerFactory.getLogger(srv_apply_join.class.getName());
    @Autowired
    akka_system akka;

    private class context{
        req_apply_join_guild req;
        String tx_token;
        ActorRef target_ref=null;
        ActorRef guild_ref=null;
    }
    public Mono serv(ServerRequest request){
        context this_context=new context();
        this_context.tx_token = srv_helper.get_transaction_token(request);
        boolean in_transaction = !this_context.tx_token.equals(static_define.no_in_transaction);
        return request.bodyToMono(req_apply_join_guild.class)
                .doOnNext(req->{
                    this_context.req=req;
                })
                .flatMap(req->{
                    return actor_player_guild_data.find_or_load(req.getTarget_namespace(),req.getTarget_id(),akka.actor_system);
                })
                .flatMap(player_ref->{
                    if(player_ref.isTerminated())
                        return Mono.error(new Throwable("cant create player actorref"));
                    this_context.target_ref=player_ref;
                    return actor_guild.find_or_load(this_context.req.getGuid_id(),akka.actor_system);
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
                                    return actor_entity.join_transaction(this_context.target_ref,this_context.tx_token)
                                            .doOnNext(ref->{
                                                this_context.target_ref=ref;
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
                .flatMap(unused->{
                    return Mono.fromCompletionStage
                            (
                                    ask
                                            (
                                                    this_context.target_ref,
                                                    (actor_player_guild_data.msg_inject_ask<player_guild_data, Integer>) data->{
                                                        if(!data.is_guild_empty())
                                                            return res_apply_join.result_has_joined;
                                                        return res_apply_join.result_ok;
                                                    },
                                                    Duration.ofSeconds(1)
                                            )
                                            .thenApply(o->{return Integer.class.cast(o);})
                            );
                })
                .flatMap(res->{
                    switch (res){
                        case res_apply_join.result_ok:
                            return Mono.just(this_context.req);
                        default:
                            return Mono.error(new throwable_res(res.toString()));
                    }
                })
                .flatMap(unused->{
                    return Mono.fromCompletionStage
                            (
                                    ask
                                            (
                                                    this_context.guild_ref,
                                                    (actor_guild.msg_inject_ask<guild, Integer>) data->{
                                                        return data.apply_join(this_context.req);
                                                    },
                                                    Duration.ofSeconds(1)
                                            )
                                            .thenApply(o->Integer.class.cast(o))
                            );
                })
                .flatMap(res->{
                    switch (res){
                        case res_apply_join.result_ok:
                            this_context.target_ref.tell(
                                    (actor_player_guild_data.msg_inject_tell<player_guild_data>) data->{
                                        data.setGuild_id(this_context.req.getGuid_id());
                                    },
                                    ActorRef.noSender()
                            );
                            return Mono.just(res);
                        default:
                            return Mono.error(new throwable_res(res.toString()));

                    }
                })
                .doOnNext(i->{
                    if(!in_transaction){
                        this_context.target_ref.tell(public_message_define.SAVE_SIGNAL,ActorRef.noSender());
                        this_context.guild_ref.tell(public_message_define.SAVE_SIGNAL,ActorRef.noSender());
                    }
                    else {
                        actor_transaction.simple_msg_push_tx_context_teller(
                                this_context.guild_ref.path().toString(),this_context.tx_token, akka.actor_system);
                        actor_transaction.simple_msg_push_tx_context_teller(
                                this_context.target_ref.path().toString(),this_context.tx_token, akka.actor_system);
                    }
                })
                .doOnError(i->{
                    if(in_transaction){
                        this_context.target_ref.tell
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
                .onErrorResume(e->{
                    if(e instanceof throwable_res)
                        return Mono.just(Integer.valueOf(e.getMessage()));
                    else
                        return Mono.error(e);
                })
                .flatMap(res->{
                    switch (res){
                        case res_apply_join.result_ok:
                            res_apply_join res1 = new res_apply_join();
                            res1.setResult_value(res);
                            return ServerResponse.ok()
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .body(BodyInserters.fromObject(res1));
                        default:
                            res_apply_join res2 = new res_apply_join();
                            res2.setResult_value(res);
                            return ServerResponse.ok()
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .body(BodyInserters.fromObject(res2));
                    }
                });
    }
}
