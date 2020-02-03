package com.cyworld.social.data.service.guild;

import com.cyworld.social.data.actors.akka_system;
import com.cyworld.social.data.actors.guild.actor_guild;
import com.cyworld.social.data.entity.guild.guild;
import com.cyworld.social.utils.request.guild.req_get_help_list;
import com.cyworld.social.utils.response.guild.res_get_guild_help_list;
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
public class srv_get_help_list {
    @Autowired
    akka_system akka;

    private class context{
        req_get_help_list req;
    }

    public Mono serv(ServerRequest request) {
        context this_context = new context();
        return request.bodyToMono(req_get_help_list.class)
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
                                                                (actor_guild.msg_inject_ask<guild, res_get_guild_help_list>) data->{
                                                                    res_get_guild_help_list res=new res_get_guild_help_list();
                                                                    if(data.find_member(this_context.req.getPlayer_namespace(),
                                                                            this_context.req.getPlayer_id())==null){
                                                                        return res;
                                                                    }
                                                                    data.help_info.build_info_list(
                                                                            this_context.req,
                                                                            res
                                                                    );
                                                                    return res;
                                                                },
                                                                Duration.ofSeconds(1)
                                                        )
                                                        .thenApply(res_get_guild_help_list.class::cast)
                                        );
                            });
                })
                .flatMap(res->{
                    return ServerResponse.ok().contentType(MediaType.APPLICATION_JSON).body(BodyInserters.fromObject(res));
                });
    }
}
