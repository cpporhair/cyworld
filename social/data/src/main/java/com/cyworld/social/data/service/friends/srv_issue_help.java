package com.cyworld.social.data.service.friends;

import akka.actor.ActorRef;
import com.cyworld.social.data.actors.akka_system;
import com.cyworld.social.data.actors.friends.actor_friends_data;
import com.cyworld.social.data.actors.public_message_define;
import com.cyworld.social.data.entity.friends.help_data;
import com.cyworld.social.data.entity.friends.player_friends_data;
import com.cyworld.social.data.mongodb.db_context;

import com.cyworld.social.utils.request.friend.req_issue_help;
import lombok.Data;
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
public class srv_issue_help {
    @Autowired
    akka_system akka;

    @Data
    private static class context{
        req_issue_help req;
        ReactiveMongoTemplate mongo;
        ActorRef player_friends_data_actor_ref;
    }

    public Mono serv(ServerRequest request){
        context this_context=new context();
        return request.bodyToMono(req_issue_help.class)
                .doOnNext(req->{
                    this_context.req=req;
                })
                .flatMap(req->{
                    return actor_friends_data.find_or_build(
                            req.getPlayer_namespace(),
                            req.getPlayer_id(),
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
                                                        (actor_friends_data.msg_inject_ask<player_friends_data,Boolean>) data->{
                                                            data.update_current_help_data();
                                                            data.setCurrent_help_data(
                                                                    help_data
                                                                            .builder()
                                                                            .help_amount(this_context.req.getAmount())
                                                                            .help_type(this_context.req.getNeed_ID())
                                                                            .timeout(System.currentTimeMillis()+this_context.req.getLifecycle())
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
