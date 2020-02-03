package com.cyworld.social.data.service.friends;


import akka.actor.ActorRef;
import com.cyworld.social.data.actors.akka_system;
import com.cyworld.social.data.actors.friends.actor_friends_data;
import com.cyworld.social.data.actors.public_message_define;
import com.cyworld.social.data.entity.friends.friend;
import com.cyworld.social.data.entity.friends.invite_message;
import com.cyworld.social.data.entity.friends.player_friends_data;
import com.cyworld.social.data.mongodb.db_context;
import com.cyworld.social.utils.request.friend.req_add_to_blacklist;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.ArrayList;

import static akka.pattern.Patterns.ask;

@Component
public class srv_add_to_blacklist {
    @Autowired
    akka_system akka;

    private static class context{
        req_add_to_blacklist req;
        ActorRef player_actor_ref;
        ActorRef friend_actor_ref;
    }
    public Mono serv(ServerRequest request){
        context this_context = new context();
        return request.bodyToMono(req_add_to_blacklist.class)
                .doOnNext(req->{
                    this_context.req=req;
                })
                .flatMap(req->{
                    return actor_friends_data.find_or_build(req.getPlayer_namespace(), req.getPlayer_id(),akka.actor_system);
                })
                .doOnNext(ref->{
                    this_context.player_actor_ref=ref;
                })
                .flatMap(ref->{
                    return actor_friends_data.find_or_build(
                            this_context.req.getFriend_namespace(),
                            this_context.req.getFriend_id(),
                            akka.actor_system);
                })
                .doOnNext(ref->{
                    this_context.friend_actor_ref=ref;
                })
                .flatMap(notused->{
                    if(this_context.player_actor_ref.isTerminated())
                        return Mono.error(new Throwable("cant create actor".concat(this_context.player_actor_ref.path().name())));
                    if(this_context.friend_actor_ref.isTerminated())
                        return Mono.error(new Throwable("cant create actor".concat(this_context.friend_actor_ref.path().name())));
                    else
                        return Mono.fromCompletionStage(
                                ask(
                                        this_context.player_actor_ref,
                                        (actor_friends_data.msg_inject_ask<player_friends_data,Boolean>) data->{
                                            data.set_friend_to_blacklist(
                                                    this_context.req.getFriend_namespace(),
                                                    this_context.req.getFriend_id()
                                            );
                                            return Boolean.TRUE;
                                        },
                                        Duration.ofSeconds(1)
                                ).thenApply(Boolean.class::cast)
                        );

                })
                .doOnNext(successed->{
                    if(successed){
                        this_context.player_actor_ref.tell(
                                public_message_define.SAVE_SIGNAL,
                                ActorRef.noSender()
                        );
                    }
                })
                .flatMap(successed->{
                    if(!successed)
                        return Mono.error(new Throwable("not invited"));
                    else
                        return Mono.fromCompletionStage(
                                ask(
                                        this_context.friend_actor_ref,
                                        (actor_friends_data.msg_inject_ask<player_friends_data,Boolean>) data->{
                                            data.remove_friend(
                                                    this_context.req.getPlayer_namespace(),
                                                    this_context.req.getPlayer_id()
                                            );
                                            return Boolean.TRUE;
                                        },
                                        Duration.ofSeconds(1)
                                ).thenApply(Boolean.class::cast)
                        );
                })
                .doOnNext(successed->{
                    if(successed){
                        this_context.friend_actor_ref.tell(
                                public_message_define.SAVE_SIGNAL,
                                ActorRef.noSender()
                        );
                    }
                })
                .flatMap(successed->{
                    if(!successed)
                        return Mono.error(new Throwable("not invited"));
                    else
                        return ServerResponse.ok().body(BodyInserters.fromObject(this_context.req));
                });
    }
}
