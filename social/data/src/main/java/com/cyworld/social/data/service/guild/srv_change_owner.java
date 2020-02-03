package com.cyworld.social.data.service.guild;

import akka.actor.ActorRef;
import com.cyworld.social.data.actors.akka_system;
import com.cyworld.social.data.actors.guild.actor_guild;
import com.cyworld.social.data.actors.public_message_define;
import com.cyworld.social.data.entity.guild.guild;
import com.cyworld.social.utils.request.guild.req_change_owner;
import com.cyworld.social.utils.response.guild.res_change_owner;
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
public class srv_change_owner {
    @Autowired
    akka_system akka;

    private class context{
        req_change_owner req;
        int owner_new_pos=0;
        ActorRef guild_ref=null;
    }

    public Mono serv(ServerRequest request){
        context this_context=new context();
        return request.bodyToMono(req_change_owner.class)
                .doOnNext(req->{
                    this_context.req=req;
                })
                .flatMap(req->{
                    return actor_guild.find_or_load(this_context.req.getGuid_id(),akka.actor_system);
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
                                                        int i=data.change_owner(this_context.req);
                                                        this_context.owner_new_pos=data.get_member_pos(
                                                                this_context.req.getPlayer_namespace(),
                                                                this_context.req.getPlayer_id()
                                                        );
                                                        return i;
                                                    },
                                                    Duration.ofSeconds(1)
                                            )
                                            .thenApply(o->{
                                                Integer res=Integer.class.cast(o);
                                                switch (res){
                                                    case res_change_owner.result_ok:
                                                        this_context.guild_ref.tell(public_message_define.SAVE_SIGNAL,ActorRef.noSender());
                                                        break;
                                                }
                                                return res;
                                            })
                            );
                })
                .flatMap(res->{
                    res_change_owner resChangeOwner=new res_change_owner();
                    resChangeOwner.setResult_value(res);
                    resChangeOwner.setOwner_new_pos(this_context.owner_new_pos);
                    return ServerResponse.ok()
                            .contentType(MediaType.APPLICATION_JSON)
                            .body(BodyInserters.fromObject(resChangeOwner));
                });
    }
}
