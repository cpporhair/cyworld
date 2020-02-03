package com.cyworld.social.data.service.guild;

import akka.actor.ActorRef;
import com.cyworld.social.data.actors.akka_system;
import com.cyworld.social.data.actors.guild.actor_guild;
import com.cyworld.social.data.actors.public_message_define;
import com.cyworld.social.data.entity.guild.guild;
import com.cyworld.social.data.utils.guiild_config_listener;
import com.cyworld.social.utils.request.guild.req_set_pos;
import com.cyworld.social.utils.response.guild.res_set_pos;
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
public class srv_set_pos {
    @Autowired
    akka_system akka;

    private class context{
        req_set_pos req;
        ActorRef target_ref=null;
        ActorRef guild_ref=null;
    }
    public Mono serv(ServerRequest request){
        context this_context=new context();
        return request.bodyToMono(req_set_pos.class)
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
                                                    (actor_guild.msg_inject_ask<guild, res_set_pos>) data->{
                                                        res_set_pos res=new res_set_pos();
                                                        res.setOld(data.get_member_pos(
                                                                this_context.req.getTarget_namespace(),
                                                                this_context.req.getTarget_id()));
                                                        res.setResult_value(
                                                                data.set_member_pos(
                                                                        this_context.req,
                                                                        this_context.req.getMax_count_lvl3(),
                                                                        this_context.req.getMax_count_lvl2())
                                                        );
                                                        if(res.getResult_value()==res_set_pos.result_ok)
                                                            res.setPos(this_context.req.getPos());
                                                        else
                                                            res.setPos(res.getOld());
                                                        return res;
                                                    },
                                                    Duration.ofSeconds(1)
                                            )
                                            .thenApply(o->{
                                                res_set_pos res=res_set_pos.class.cast(o);
                                                switch (res.getResult_value()){
                                                    case res_set_pos.result_ok:
                                                        this_context.guild_ref.tell(public_message_define.SAVE_SIGNAL,ActorRef.noSender());
                                                        break;
                                                }
                                                return res;
                                            })
                            );
                })
                .flatMap(res->{
                    return ServerResponse.ok()
                            .contentType(MediaType.APPLICATION_JSON)
                            .body(BodyInserters.fromObject(res));
                });
    }
}
