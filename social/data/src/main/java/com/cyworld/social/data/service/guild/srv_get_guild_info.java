package com.cyworld.social.data.service.guild;

import akka.actor.ActorRef;
import com.alibaba.fastjson.JSON;
import com.cyworld.social.data.actors.akka_system;
import com.cyworld.social.data.actors.guild.actor_guild;
import com.cyworld.social.data.actors.guild.actor_player_guild_data;
import com.cyworld.social.data.actors.public_message_define;
import com.cyworld.social.data.entity.guild.guild;
import com.cyworld.social.data.entity.guild.guild_member;
import com.cyworld.social.data.entity.guild.player_guild_data;
import com.cyworld.social.utils.request.guild.req_get_guild_info;
import com.cyworld.social.utils.response.guild.res_get_guild_info;
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
public class srv_get_guild_info {
    @Autowired
    akka_system akka;

    private class context{
        req_get_guild_info req;
        ActorRef player_ref=null;
        ActorRef guild_ref=null;
    }

    public Mono serv(ServerRequest request) {
        context this_context = new context();
        return request.bodyToMono(req_get_guild_info.class)
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
                                                    (actor_guild.msg_inject_ask<guild, res_get_guild_info>) data->{
                                                        res_get_guild_info res=new res_get_guild_info();
                                                        if(data.find_member(this_context.req.getPlayer_namespace(),this_context.req.getPlayer_id())==null){
                                                            data.clear_all_member_info(this_context.req.getPlayer_namespace(),this_context.req.getPlayer_id());
                                                            res.setEmpty_flag(1);
                                                            this_context.guild_ref.tell
                                                                    (
                                                                            public_message_define.SAVE_SIGNAL,
                                                                            ActorRef.noSender()
                                                                    );
                                                        }
                                                        else{
                                                            res.setEmpty_flag(0);
                                                            data.fill_res_get_guild_info(
                                                                    res,
                                                                    this_context.req.getPlayer_namespace(),
                                                                    this_context.req.getPlayer_id());
                                                        }
                                                        return res;
                                                    },
                                                    Duration.ofSeconds(1)
                                            )
                                            .thenApply(res_get_guild_info.class::cast)
                            );
                })
                .doOnNext(res->{
                    if(res.getEmpty_flag()==0)
                        return;
                    actor_player_guild_data.find_or_load(
                            this_context.req.getPlayer_namespace(),
                            this_context.req.getPlayer_id(),
                            akka.actor_system
                    )
                            .doOnNext(ref->{
                                ref.tell
                                        (
                                                (actor_player_guild_data.msg_inject_tell<player_guild_data>) data->{
                                                    data.setGuild_id("NULL");
                                                },
                                                ActorRef.noSender()
                                        );
                                ref.tell
                                        (
                                                public_message_define.SAVE_SIGNAL,
                                                ActorRef.noSender()
                                        );
                            })
                            .subscribe();
                })
                .flatMap(res->{
                    return ServerResponse.ok().contentType(MediaType.APPLICATION_JSON).body(BodyInserters.fromObject(res));
                });
    }
}
