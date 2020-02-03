package com.cyworld.social.data.service.ranks;

import akka.actor.ActorRef;
import com.cyworld.social.data.actors.akka_system;
import com.cyworld.social.data.actors.ranks.actor_ranks;
import com.cyworld.social.data.entity.ranks.ranks;
import com.cyworld.social.data.entity.ranks.ranks_data;
import com.cyworld.social.data.mongodb.db_context;
import com.cyworld.social.utils.request.ranks.req_get_ranks;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.List;
import java.util.TreeSet;
import java.util.stream.Collectors;

import static akka.pattern.Patterns.ask;

@Component
public class srv_get_ranks {
    @Autowired
    akka_system akka;

    private class context{
        req_get_ranks req;
        ActorRef actor_ref;
    }

    public Mono serv(ServerRequest request){
        context this_context = new context();
        return request.bodyToMono(req_get_ranks.class)
                .doOnNext(req->{
                    this_context.req=req;
                })
                .flatMap(req->{
                    return actor_ranks.find_or_build(req.getType(),akka.actor_system);
                })
                .doOnNext(ref->{
                    this_context.actor_ref=ref;
                })
                .flatMap(ref->{
                    if (ref.isTerminated())
                        return Mono.error(new Throwable("cant create actor".concat(ref.path().name())));
                    else
                        return Mono.fromCompletionStage
                                (
                                        ask
                                                (
                                                        this_context.actor_ref,
                                                        (actor_ranks.msg_inject_ask<ranks, List<String>>) data->{
                                                            return data.getDatas()
                                                                    .stream()
                                                                    .skip(this_context.req.getSkip())
                                                                    .limit(this_context.req.getTake())
                                                                    .map(o->{return o.getInner_data();})
                                                                    .collect(Collectors.toList());
                                                        },
                                                        Duration.ofSeconds(1)
                                                )
                                                .thenApply(o->{return List.class.cast(o);})

                                );
                })
                .flatMap(res->{
                    return ServerResponse.ok().body(BodyInserters.fromObject(res));
                });
    }
}
