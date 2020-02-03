package com.cyworld.social.data.service.friends;

import akka.actor.ActorRef;
import com.alibaba.fastjson.JSON;
import com.cyworld.social.data.actors.akka_system;
import com.cyworld.social.data.actors.friends.actor_friends_data;
import com.cyworld.social.data.entity.friends.help_data;
import com.cyworld.social.data.entity.friends.player_friends_data;
import com.cyworld.social.data.mongodb.db_context;
import com.cyworld.social.utils.request.friend.req_get_my_help;
import com.cyworld.social.utils.response.friend.res_get_my_help;
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
public class srv_get_my_help {
    @Autowired
    akka_system akka;

    @Data
    private static class context{
        req_get_my_help req;
        ActorRef player_ref;
    }
    public Mono serv(ServerRequest request) {
        context this_context = new context();
        return request.bodyToMono(req_get_my_help.class)
                .doOnNext(req -> {
                    this_context.req = req;
                })
                .flatMap(req -> {
                    return actor_friends_data.find_or_build(
                            req.getPlayer_namespace(),
                            req.getPlayer_id(),
                            akka.actor_system
                    );
                })
                .doOnNext(ref -> {
                    this_context.player_ref = ref;
                })
                .flatMap(ref->{
                    if (ref.isTerminated())
                        return Mono.error(new Throwable("cant create actor".concat(ref.path().name())));
                    else
                        return Mono.fromCompletionStage
                                (
                                        ask
                                                (
                                                        this_context.player_ref,
                                                        (actor_friends_data.msg_inject_ask<player_friends_data, res_get_my_help>) data->{
                                                            data.update_current_help_data();
                                                            res_get_my_help res=new res_get_my_help();
                                                            if(data.getCurrent_help_data()==null)
                                                                return res;
                                                            res.setHelp_amount(data.getCurrent_help_data().getHelp_amount());
                                                            res.setHelp_type(data.getCurrent_help_data().getHelp_type());
                                                            res.setTime(data.getCurrent_help_data().getTimeout());

                                                            return res;
                                                        },
                                                        Duration.ofSeconds(1)
                                                )
                                                .thenApply(o->{return res_get_my_help.class.cast(o);})

                                )
                                .map(res->{
                                    return JSON.toJSONString(res);
                                });
                })
                .flatMap(res->{
                    return ServerResponse.ok().body(BodyInserters.fromObject(res));
                });
    }
}
