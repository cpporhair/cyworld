package com.cyworld.social.data.service.friends;

import akka.actor.ActorRef;
import com.cyworld.social.data.actors.akka_system;
import com.cyworld.social.data.actors.friends.actor_friends_data;
import com.cyworld.social.data.actors.public_message_define;
import com.cyworld.social.data.entity.friends.invite_message;
import com.cyworld.social.data.entity.friends.player_friends_data;
import com.cyworld.social.data.mongodb.db_context;
import com.cyworld.social.utils.request.friend.req_friends_invite;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

import java.time.Duration;

import static akka.pattern.Patterns.ask;
@Component
public class srv_invite {

    @Autowired
    akka_system akka;

    private class context{
        req_friends_invite req;
        ActorRef player_friends_data_actor_ref;
    }

    public Mono serv(ServerRequest request){
        srv_invite.context this_context = new srv_invite.context();
        return request.bodyToMono(req_friends_invite.class)
                .doOnNext(req->{
                    this_context.req=req;
                })
                .flatMap(req->{
                    return actor_friends_data.find_or_build(
                            req.getTarget_namespace(),
                            req.getTarget_id(),
                            akka.actor_system
                    );
                })
                .doOnNext(ref->{
                    this_context.player_friends_data_actor_ref=ref;
                })
                .flatMap(ref->{
                    if (ref.isTerminated())
                        return Mono.error(new Throwable("cant create actor".concat(ref.path().name())));
                    else
                        return Mono.fromCompletionStage
                                (
                                        ask
                                                (
                                                        this_context.player_friends_data_actor_ref,
                                                        (actor_friends_data.msg_inject_ask<player_friends_data,Boolean>)data->{
                                                            if(data
                                                                    .getAll_invite()
                                                                    .stream()
                                                                    .anyMatch(o->{
                                                                        return o.getFrom_id().equals(this_context.req.getFrom_id())
                                                                                && o.getFrom_namespace().equals(this_context.req.getFrom_namespace());
                                                                    })){
                                                                return Boolean.FALSE;
                                                            }
                                                            data.getAll_invite().add(
                                                                    invite_message
                                                                            .builder()
                                                                            .type("0")
                                                                            .from_id(this_context.req.getFrom_id())
                                                                            .from_namespace(this_context.req.getFrom_namespace())
                                                                            .invite_time(System.currentTimeMillis())
                                                                            .build()
                                                            );
                                                            return Boolean.TRUE;
                                                        },
                                                        Duration.ofSeconds(1)
                                                )
                                        .thenApply(o->{return Boolean.class.cast(o);})

                                );
                })
                .doOnNext(res->{
                    if(res){
                        this_context.player_friends_data_actor_ref.tell(
                                public_message_define.SAVE_SIGNAL,
                                ActorRef.noSender()
                        );
                    }
                })
                .flatMap(res->{
                    return ServerResponse.ok().body(BodyInserters.fromObject(this_context.req));
                });
    }
}
