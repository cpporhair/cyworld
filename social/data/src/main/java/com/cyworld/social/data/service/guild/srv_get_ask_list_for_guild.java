package com.cyworld.social.data.service.guild;

import akka.actor.ActorRef;
import com.cyworld.social.data.actors.akka_system;
import com.cyworld.social.data.actors.guild.actor_guild;
import com.cyworld.social.data.entity.guild.guild;
import com.cyworld.social.utils.request.guild.req_get_ask_list_for_guild;
import com.cyworld.social.utils.response.guild.res_get_ask_list_component;
import com.cyworld.social.utils.response.guild.res_get_ask_list_guild;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.stream.Collectors;

import static akka.pattern.Patterns.ask;
@Component
public class srv_get_ask_list_for_guild {
    @Autowired
    akka_system akka;

    private class context{
        req_get_ask_list_for_guild req;
        ActorRef guild_ref=null;
    }
    public Mono serv(ServerRequest request) {
        context this_context = new context();
        return request.bodyToMono(req_get_ask_list_for_guild.class)
                .doOnNext(req->{
                    this_context.req=req;
                })
                .flatMap(req->{
                    return actor_guild.find_or_load(this_context.req.getGuild_id(),akka.actor_system);
                })
                .flatMap(ref->{
                    if(ref.isTerminated())
                        return Mono.error(new Throwable("cant find guild actor"));
                    this_context.guild_ref=ref;
                    return Mono.fromCompletionStage
                            (
                                    ask
                                            (
                                                    this_context.guild_ref,
                                                    (actor_guild.msg_inject_ask<guild, res_get_ask_list_guild>) data->{
                                                        res_get_ask_list_guild res=new res_get_ask_list_guild();
                                                        data.clear_expiration_ask_list();
                                                        res.components=data.ask_list.stream().map(e->{
                                                            res_get_ask_list_component res_get=new res_get_ask_list_component();
                                                            res_get.setJson_game_data("");
                                                            res_get.setPlayer_id(e.getSrc_id());
                                                            res_get.setPlayer_namespace(e.getSrc_namespace());
                                                            res_get.setPower(e.getPower());
                                                            res_get.setTimer(e.getExpiration()-24*3600*1000);
                                                            return res_get;
                                                        }).collect(Collectors.toList());
                                                        return res;
                                                    },
                                                    Duration.ofSeconds(1)
                                            )
                                            .thenApply(res_get_ask_list_guild.class::cast)
                            );
                })
                .flatMap(res->{
                    return ServerResponse.ok().contentType(MediaType.APPLICATION_JSON).body(BodyInserters.fromObject(res));
                });
    }
}
