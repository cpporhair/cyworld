package com.cyworld.social.data.service.guild;

import akka.actor.ActorRef;
import com.cyworld.social.data.actors.akka_system;
import com.cyworld.social.data.actors.guild.actor_guild;
import com.cyworld.social.data.actors.public_message_define;
import com.cyworld.social.data.entity.guild.guild;
import com.cyworld.social.utils.request.guild.req_issue_help;
import com.cyworld.social.utils.response.guild.res_issue_help;
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
public class srv_guild_issue_help {
    @Autowired
    akka_system akka;

    private class context{
        req_issue_help req;
    }

    public Mono serv(ServerRequest request) {
        context this_context = new context();
        return request.bodyToMono(req_issue_help.class)
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
                                                                (actor_guild.msg_inject_ask<guild, res_issue_help>) data->{
                                                                    res_issue_help res=new res_issue_help();
                                                                    if(data.find_member(this_context.req.getPlayer_namespace(),
                                                                            this_context.req.getPlayer_id())==null){
                                                                        res.setResult_value(res_issue_help.result_not_member);
                                                                        return res;
                                                                    }
                                                                    res.setResult_value(data.help_info.issue_help(this_context.req));
                                                                    return res;
                                                                },
                                                                Duration.ofSeconds(1)
                                                        )
                                                        .thenApply(res_issue_help.class::cast)
                                        )
                                        .doOnNext(res->{
                                            switch (res.getResult_value()){
                                                case res_issue_help.result_ok:
                                                    ref.tell(public_message_define.SAVE_SIGNAL,ActorRef.noSender());
                                                    return;
                                            }
                                        });
                            });
                })
                .flatMap(res->{
                    return ServerResponse.ok().contentType(MediaType.APPLICATION_JSON).body(BodyInserters.fromObject(res));
                });
    }
}
