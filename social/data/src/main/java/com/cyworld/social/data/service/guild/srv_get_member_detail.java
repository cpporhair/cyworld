package com.cyworld.social.data.service.guild;

import akka.actor.ActorRef;
import com.cyworld.social.data.actors.akka_system;
import com.cyworld.social.data.actors.guild.actor_guild;
import com.cyworld.social.data.entity.guild.guild;
import com.cyworld.social.utils.request.guild.req_get_member_detail;
import com.cyworld.social.utils.response.guild.res_get_members_component;
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
public class srv_get_member_detail {
    @Autowired
    akka_system akka;

    private class context{
        req_get_member_detail req;
        ActorRef player_ref=null;
        ActorRef guild_ref=null;
    }

    public Mono serv(ServerRequest request) {
        context this_context = new context();
        return request.bodyToMono(req_get_member_detail.class)
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
                                                    (actor_guild.msg_inject_ask<guild, res_get_members_component>) data->{
                                                        res_get_members_component c=new res_get_members_component();
                                                        data.members.stream().forEach(o->{
                                                            if(o.getPlayer_id().equals(this_context.req.getPlayer_id())
                                                            && o.getPlayer_namespace().equals(this_context.req.getPlayer_namespace())){
                                                                data.update_member_donate(o,this_context.req);
                                                                c.setDonate(o.getDonate_log().target.getDonate().toJSONString());
                                                                c.setExp(o.getDonate_experience().target.getExp());
                                                                c.setId(o.getPlayer_id());
                                                                c.setNamespace(o.getPlayer_namespace());
                                                                c.setPos(String.valueOf(o.getPosition()));
                                                                c.setPower(o.getPower());
                                                            }
                                                        });;
                                                        return c;
                                                    },
                                                    Duration.ofSeconds(1)
                                            )
                                            .thenApply(res_get_members_component.class::cast)
                            );
                })
                .flatMap(res->{
                    return ServerResponse.ok().contentType(MediaType.APPLICATION_JSON).body(BodyInserters.fromObject(res));
                });
    }
}
