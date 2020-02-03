package com.cyworld.social.data.service.guild;

import akka.actor.ActorRef;
import com.cyworld.social.data.actors.akka_system;
import com.cyworld.social.data.actors.guild.actor_guild;
import com.cyworld.social.data.actors.public_message_define;
import com.cyworld.social.data.entity.guild.guild;
import com.cyworld.social.utils.request.guild.req_reject_join;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

import static akka.pattern.Patterns.ask;
@Component
public class srv_reject_join {
    private static final Logger logger = LoggerFactory.getLogger(srv_reject_join.class.getName());
    @Autowired
    akka_system akka;

    private class context{
        req_reject_join req;
        ActorRef guild_ref=null;
    }
    public Mono serv(ServerRequest request){
        context this_context=new context();
        return request.bodyToMono(req_reject_join.class)
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
                    this_context.guild_ref.tell
                            (
                                    (actor_guild.msg_inject_tell<guild>) data->{
                                        if(data.reject_join(this_context.req))
                                            this_context.guild_ref.tell(public_message_define.SAVE_SIGNAL,ActorRef.noSender());
                                    },
                                    ActorRef.noSender()
                            );
                    return ServerResponse.ok().body(BodyInserters.fromObject("OK"));
                });
    }
}
