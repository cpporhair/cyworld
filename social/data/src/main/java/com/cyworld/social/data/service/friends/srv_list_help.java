package com.cyworld.social.data.service.friends;

import akka.actor.ActorRef;
import com.alibaba.fastjson.JSON;
import com.cyworld.social.data.actors.akka_system;
import com.cyworld.social.data.actors.friends.actor_friends_data;
import com.cyworld.social.data.entity.friends.help_data;
import com.cyworld.social.data.entity.friends.player_friends_data;
import com.cyworld.social.data.mongodb.db_context;
import com.cyworld.social.utils.request.friend.req_list_help;
import lombok.Data;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static akka.pattern.Patterns.ask;
@Component
public class srv_list_help {
    @Autowired
    akka_system akka;

    @Data
    private static class context{
        req_list_help req;
        ActorRef player_friends_data_actor_ref;
    }
    public Mono serv(ServerRequest request){
        context this_context=new context();
        return request.bodyToMono(req_list_help.class)
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
                                                        (actor_friends_data.msg_inject_ask<player_friends_data, List<String[]>>) data->{
                                                            return data.getFriends()
                                                                    .stream()
                                                                    .map(o->{
                                                                        String[] r=new String[2];
                                                                        r[0]=o.getNamespace();
                                                                        r[1]=o.get_id();
                                                                        return r;
                                                                    })
                                                                    .collect(Collectors.toList());
                                                        },
                                                        Duration.ofSeconds(1)
                                                )
                                                .thenApply(o->{return (List<String[]>)(o);})

                                );
                })
                .flatMap(res->{
                    return act_foreach_friend.get_help_data(
                            this_context.req.getPlayer_namespace().concat(".").concat(this_context.req.getPlayer_id()),
                            res,
                            akka.actor_system
                    );
                })
                .flatMap(res->{
                    return ServerResponse.ok().contentType(MediaType.APPLICATION_JSON).body(BodyInserters.fromObject(res));
                });
    }
}
