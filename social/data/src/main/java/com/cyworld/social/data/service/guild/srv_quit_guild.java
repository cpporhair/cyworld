package com.cyworld.social.data.service.guild;

import akka.actor.ActorRef;
import com.cyworld.social.data.actors.akka_system;
import com.cyworld.social.data.actors.guild.actor_guild;
import com.cyworld.social.data.actors.guild.actor_player_guild_data;
import com.cyworld.social.data.actors.public_message_define;
import com.cyworld.social.data.entity.guild.guild;
import com.cyworld.social.data.entity.guild.player_guild_data;
import com.cyworld.social.utils.request.guild.req_quit_guild;
import com.cyworld.social.utils.response.common.throwable_res;
import com.cyworld.social.utils.response.guild.res_quit_guild;
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
public class srv_quit_guild {
    @Autowired
    akka_system akka;

    private class context{
        req_quit_guild req;
        ActorRef target_ref=null;
        ActorRef guild_ref=null;
    }

    public Mono serv(ServerRequest request){
        context this_context=new context();
        return request.bodyToMono(req_quit_guild.class)
                .doOnNext(req->{
                    this_context.req=req;
                })
                .flatMap(req->{
                    return actor_player_guild_data.find_or_load(req.getPlayer_namespace(),req.getPlayer_id(),akka.actor_system);
                })
                .flatMap(ref->{
                    if(ref.isTerminated())
                        return Mono.error(new Throwable("cant find target actor"));
                    this_context.target_ref=ref;
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
                                                    (actor_guild.msg_inject_ask<guild, Integer>) data->{
                                                        return data.quit(this_context.req);
                                                    },
                                                    Duration.ofSeconds(1)
                                            )
                                            .thenApply(o->{
                                                Integer res=Integer.class.cast(o);
                                                return res;
                                            })
                            );
                })
                .onErrorResume(e->{
                    if(e instanceof throwable_res)
                        return Mono.just(Integer.valueOf(e.getMessage()));
                    else
                        return Mono.error(e);
                })
                .flatMap(res->{
                    switch (res){
                        case res_quit_guild.result_ok:
                            this_context.guild_ref.tell
                                    (
                                            public_message_define.SAVE_SIGNAL,
                                            ActorRef.noSender()
                                    );
                            this_context.target_ref.tell
                                    (
                                            (actor_player_guild_data.msg_inject_tell<player_guild_data>) data->{
                                                data.setGuild_id("NULL");
                                            },
                                            ActorRef.noSender()
                                    );
                            this_context.target_ref.tell
                                    (
                                            public_message_define.SAVE_SIGNAL,
                                            ActorRef.noSender()
                                    );
                            break;
                    }
                    return ServerResponse.ok()
                            .contentType(MediaType.APPLICATION_JSON)
                            .body(BodyInserters.fromObject(new res_quit_guild(res)));
                });
    }
}
