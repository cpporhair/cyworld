package com.cyworld.social.data.service.guild;

import akka.actor.ActorRef;
import com.cyworld.social.data.actors.akka_system;
import com.cyworld.social.data.actors.guild.actor_guild;
import com.cyworld.social.data.actors.public_message_define;
import com.cyworld.social.data.entity.guild.guild;
import com.cyworld.social.utils.request.guild.req_del_guild_help;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

import java.time.Duration;

import static akka.pattern.Patterns.ask;

@Component
public class srv_del_guild_help {
    @Autowired
    akka_system akka;

    private class context{
        req_del_guild_help req;
    }

    public Mono serv(ServerRequest request) {
        context this_context = new context();
        return request.bodyToMono(req_del_guild_help.class)
                .doOnNext(req -> {
                    this_context.req = req;
                })
                .flatMap(req->{
                    return actor_guild.find_or_load(req.getGuild_id(),akka.actor_system)
                            .flatMap(ref->{
                                return Mono.fromCompletionStage
                                        (
                                                ask
                                                        (
                                                                ref,
                                                                (actor_guild.msg_inject_ask<guild, Boolean>) data->{
                                                                    if(data.find_member(this_context.req.getTarget_namespace(),
                                                                            this_context.req.getTarget_id())==null){
                                                                        return Boolean.FALSE;
                                                                    }
                                                                    data.help_info.del_guild_help(req);
                                                                    return Boolean.TRUE;
                                                                },
                                                                Duration.ofSeconds(1)
                                                        )
                                                        .thenApply(Boolean.class::cast)
                                        )
                                        .doOnNext(res->{
                                            if(res)
                                                ref.tell(public_message_define.SAVE_SIGNAL, ActorRef.noSender());
                                        });
                            });
                })
                .flatMap(res->{
                    return ServerResponse.ok().body(BodyInserters.fromObject(res.toString()));
                });
    }
}
