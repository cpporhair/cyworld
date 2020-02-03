package com.cyworld.social.data.service.guild;

import akka.actor.ActorRef;
import com.cyworld.social.data.actors.akka_system;
import com.cyworld.social.data.actors.guild.actor_guild;
import com.cyworld.social.data.actors.public_message_define;
import com.cyworld.social.data.entity.guild.guild;
import com.cyworld.social.utils.request.guild.req_do_guild_help;
import com.cyworld.social.utils.response.guild.res_do_guild_help;
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
public class srv_do_guild_help {
    @Autowired
    akka_system akka;

    private class context{
        req_do_guild_help req;
    }

    public Mono serv(ServerRequest request) {
        context this_context = new context();
        return request.bodyToMono(req_do_guild_help.class)
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
                                                                (actor_guild.msg_inject_ask<guild, res_do_guild_help>) data->{
                                                                    res_do_guild_help res=new res_do_guild_help();
                                                                    res.setExp(data.exp);
                                                                    if(data.find_member(this_context.req.getPlayer_namespace(),
                                                                            this_context.req.getPlayer_id())==null){
                                                                        res.setResult_value(res_do_guild_help.not_member);
                                                                        return res;
                                                                    }
                                                                    res.setResult_value(data.help_info.do_guild_help(this_context.req));
                                                                    if(res.getResult_value()==res_do_guild_help.result_ok)
                                                                        res.setCoin_able(
                                                                                data.add_coin(
                                                                                        this_context.req.getPlayer_namespace(),
                                                                                        this_context.req.getPlayer_id(),
                                                                                        this_context.req.getPlayer_coin_reset_timer(),
                                                                                        data.help_info.get_max_coin_count(
                                                                                                this_context.req.getTarget_namespace(),
                                                                                                this_context.req.getTarget_id(),
                                                                                                this_context.req.getTarget_help_id()
                                                                                        )
                                                                                )
                                                                        );
                                                                    else
                                                                        res.setCoin_able(false);
                                                                    return res;
                                                                },
                                                                Duration.ofSeconds(1)
                                                        )
                                                        .thenApply(res_do_guild_help.class::cast)
                                        )
                                        .doOnNext(res->{
                                            if(res.getResult_value()==res_do_guild_help.result_ok)
                                                ref.tell(public_message_define.SAVE_SIGNAL, ActorRef.noSender());
                                        });
                            });
                })
                .flatMap(res->{
                    return ServerResponse.ok().contentType(MediaType.APPLICATION_JSON).body(BodyInserters.fromObject(res));
                });
    }
}
