package com.cyworld.social.data.service.guild;

import akka.actor.ActorRef;
import com.cyworld.social.data.actors.akka_system;
import com.cyworld.social.data.actors.guild.actor_guild;
import com.cyworld.social.data.entity.guild.guild;
import com.cyworld.social.utils.request.guild.req_add_log;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

import java.time.Duration;

import static akka.pattern.Patterns.ask;
@Component
public class srv_add_log {
    @Autowired
    akka_system akka;

    private class context{
        req_add_log req;
        ActorRef guild_ref=null;
    }

    public Mono serv(ServerRequest request){
        context this_context=new context();
        return request.bodyToMono(req_add_log.class)
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
                                                    (actor_guild.msg_inject_ask<guild, Integer>) data->{
                                                        data.add_log(this_context.req);
                                                        return 0;
                                                    },
                                                    Duration.ofSeconds(1)
                                            )
                                            .thenApply(o->{
                                                Integer res=Integer.class.cast(o);
                                                return res;
                                            })
                            );
                })
                .flatMap(res->{
                    return ServerResponse.ok()
                            .body(BodyInserters.fromObject("OK"));
                });
    }
}
