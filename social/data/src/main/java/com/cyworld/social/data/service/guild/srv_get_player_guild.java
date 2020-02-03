package com.cyworld.social.data.service.guild;

import akka.actor.ActorRef;
import com.alibaba.fastjson.JSON;
import com.cyworld.social.data.actors.akka_system;
import com.cyworld.social.data.actors.guild.actor_player_guild_data;
import com.cyworld.social.data.entity.guild.player_guild_data;
import com.cyworld.social.utils.request.guild.req_get_player_guild;
import com.cyworld.social.utils.response.guild.res_get_player_guild;
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
public class srv_get_player_guild {
    @Autowired
    akka_system akka;

    private class context{
        req_get_player_guild req;
        res_get_player_guild res;
        ActorRef player_ref=null;
        ActorRef guild_ref=null;
    }

    public Mono serv(ServerRequest request) {
        context this_context = new context();
        return request.bodyToMono(req_get_player_guild.class)
                .doOnNext(req->{
                    this_context.req=req;
                })
                .flatMap(req->{
                    return actor_player_guild_data.find_or_load(
                            this_context.req.getPlayer_namespace(),
                            this_context.req.getPlayer_id(),
                            akka.actor_system
                    );
                })
                .flatMap(ref->{
                    return Mono.fromCompletionStage
                            (
                                    ask
                                            (
                                                    ref,
                                                    (actor_player_guild_data.msg_inject_ask<player_guild_data, res_get_player_guild>) data->{
                                                        res_get_player_guild res=new res_get_player_guild();
                                                        res.setGuild_id(data.getGuild_id());
                                                        return res;
                                                    },
                                                    Duration.ofSeconds(1)
                                            )
                                            .thenApply(res_get_player_guild.class::cast)
                            );
                })
                .switchIfEmpty(
                        Mono.just(0)
                                .map(i->{
                                    res_get_player_guild res=new res_get_player_guild();
                                    res.setGuild_id("-1");
                                    return res;
                                })
                )
                .flatMap(res->{
                    return ServerResponse.ok().contentType(MediaType.APPLICATION_JSON).body(BodyInserters.fromObject(res));
                });
    }
}
