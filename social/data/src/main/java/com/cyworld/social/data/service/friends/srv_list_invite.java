package com.cyworld.social.data.service.friends;

import akka.actor.ActorRef;
import com.alibaba.fastjson.JSON;
import com.cyworld.social.data.actors.akka_system;
import com.cyworld.social.data.actors.friends.actor_friends_data;
import com.cyworld.social.data.entity.friends.player_friends_data;
import com.cyworld.social.data.mongodb.db_context;
import com.cyworld.social.utils.request.friend.req_list_invite;
import com.cyworld.social.utils.response.friend.res_one_invite;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.List;
import java.util.stream.Collectors;

import static akka.pattern.Patterns.ask;
@Component
public class srv_list_invite {
    @Autowired
    akka_system akka;

    private class context{
        req_list_invite req;
        ActorRef player_friends_data_actor_ref;
    }

    public Mono serv(ServerRequest request){
        context this_context = new context();
        return request.bodyToMono(req_list_invite.class)
                .doOnNext(req->{
                    this_context.req=req;
                })
                .flatMap(req->{
                    return actor_friends_data.find_or_build(req.getPlayer_namespace(), req.getPlayer_id(),akka.actor_system);
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
                                                        (actor_friends_data.msg_inject_ask<player_friends_data, List<String>>) data->{
                                                            return data
                                                                    .getAll_invite()
                                                                    .stream()
                                                                    .map(o->{
                                                                        return JSON.toJSONString
                                                                                (
                                                                                        new res_one_invite(
                                                                                                o.getFrom_id(),
                                                                                                o.getFrom_namespace(),
                                                                                                o.getType(),
                                                                                                o.getInvite_time()
                                                                                        )
                                                                                );
                                                                    })
                                                                    .collect(Collectors.toList());
                                                        },
                                                        Duration.ofSeconds(1)
                                                )
                                                .thenApply(o->{return List.class.cast(o);})

                                );
                })
                .flatMap(res->{
                    return ServerResponse.ok().contentType(MediaType.APPLICATION_JSON).body(BodyInserters.fromObject(res));
                });
    }
}
