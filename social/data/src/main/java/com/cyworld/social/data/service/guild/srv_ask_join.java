package com.cyworld.social.data.service.guild;

import akka.actor.ActorRef;
import com.cyworld.social.data.actors.akka_system;
import com.cyworld.social.data.actors.guild.actor_guild;
import com.cyworld.social.data.actors.guild.actor_player_guild_data;
import com.cyworld.social.data.actors.public_message_define;
import com.cyworld.social.data.entity.guild.*;
import com.cyworld.social.utils.request.guild.req_ask_join;
import com.cyworld.social.utils.response.guild.res_ask_join_guild;
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
public class srv_ask_join {
    private static final Logger logger = LoggerFactory.getLogger(srv_ask_join.class.getName());
    @Autowired
    akka_system akka;

    private class context{
        req_ask_join req;
        ActorRef guild_ref =null;
        ActorRef player_ref=null;
    }

    public Mono serv(ServerRequest request){
        context this_context = new context();
        return request.bodyToMono(req_ask_join.class)
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
                .flatMap(ref->{
                    if(ref.isTerminated())
                        return Mono.error(new Throwable("cant create player actorref"));
                    this_context.player_ref=ref;
                    return Mono.fromCompletionStage
                            (
                                    ask
                                            (
                                                    ref,
                                                    (actor_player_guild_data.msg_inject_ask<player_guild_data,Integer>)data->{
                                                        if(!data.is_guild_empty())
                                                            return res_ask_join_guild.result_has_joined;
                                                        else
                                                            return res_ask_join_guild.result_ok;
                                                    },
                                                    Duration.ofSeconds(1)
                                            )
                                            .thenApply(Integer.class::cast)
                            );
                })
                .flatMap(res->{
                    switch (res){
                        case res_ask_join_guild.result_ok:
                            return Mono.just(this_context.req);
                        default:
                            return ServerResponse.ok()
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .body(BodyInserters.fromObject(new res_ask_join_guild(res,"")))
                                    .flatMap(s->{
                                        return Mono.empty();
                                    });
                    }
                })
                .flatMap(req->{
                    return actor_guild.find_or_load(req.getGuild_id(),akka.actor_system)
                            .flatMap(ref->{
                                if(ref.isTerminated())
                                    return Mono.error(new Throwable("cant find guild actorref"));
                                else
                                    return Mono.just(ref);
                            });
                })
                .flatMap(ref->{
                    this_context.guild_ref =ref;
                    return Mono.fromCompletionStage
                            (
                                    ask
                                            (
                                                    this_context.guild_ref,
                                                    (actor_guild.msg_inject_ask<guild, Integer>) data->{
                                                        return data.ask_join(this_context.req);
                                                    },
                                                    Duration.ofSeconds(1)
                                            )
                                            .thenApply(o->{
                                                Integer res=Integer.class.cast(o);
                                                switch (res){
                                                    case res_ask_join_guild.result_ok:
                                                        this_context.guild_ref.tell(public_message_define.SAVE_SIGNAL,ActorRef.noSender());
                                                        break;
                                                }
                                                return res;
                                            })
                            );
                })
                .flatMap(res->{
                    switch (res){
                        case res_ask_join_guild.result_ok:
                            return ServerResponse.ok()
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .body(BodyInserters.fromObject(new res_ask_join_guild(res,"")));
                        default:
                            return ServerResponse.ok()
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .body(BodyInserters.fromObject(new res_ask_join_guild(res,"")));
                    }
                });
    }
}
