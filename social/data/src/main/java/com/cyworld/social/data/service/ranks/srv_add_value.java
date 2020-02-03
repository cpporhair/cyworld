package com.cyworld.social.data.service.ranks;

import akka.actor.ActorRef;
import com.cyworld.social.data.actors.akka_system;
import com.cyworld.social.data.actors.public_message_define;
import com.cyworld.social.data.actors.ranks.actor_ranks;
import com.cyworld.social.data.entity.ranks.ranks;
import com.cyworld.social.data.entity.ranks.ranks_data;
import com.cyworld.social.utils.request.ranks.req_add_ranks_data;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

import java.time.Duration;

import static akka.pattern.Patterns.ask;
@Component
public class srv_add_value {
    private static final Logger logger = LoggerFactory.getLogger(srv_add_value.class.getName());
    @Autowired
    akka_system akka;

    private class context{
        req_add_ranks_data req;
        ActorRef actor_ref;
    }

    public Mono serv(ServerRequest request){
        context this_context = new context();
        return request.bodyToMono(req_add_ranks_data.class)
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
                                                        (actor_ranks.msg_inject_ask<ranks,Boolean>) data->{
                                                            data.getDatas().removeIf(o->{
                                                                return o.getID().equals(this_context.req.getID());
                                                            });
                                                            ranks_data new_data=ranks_data
                                                                    .builder()
                                                                    .compare_value(this_context.req.getCompare_value())
                                                                    .inner_data(this_context.req.getData())
                                                                    .ID(this_context.req.getID())
                                                                    .time(System.currentTimeMillis())
                                                                    .build();

                                                            /*
                                                            if((data.getDatas().size()>this_context.req.getMax_count())
                                                                    &&(new_data.compareTo(data.getDatas().last())<=0)
                                                            )
                                                                return Boolean.FALSE;
                                                                */
                                                            data.getDatas()
                                                                    .removeIf(o->{
                                                                        return o.getID().equals(new_data.getID());
                                                                    });
                                                            data.getDatas().add(new_data);
                                                            if(data.getDatas().size()>this_context.req.getMax_count())
                                                                data.getDatas().pollLast();
                                                            return Boolean.TRUE;
                                                            /*
                                                            if(data.getDatas().size()>this_context.req.getMax_count()){
                                                                if(new_data.compareTo(data.getDatas().last())>0){
                                                                    data.getDatas().add(new_data);
                                                                    data.getDatas()
                                                                            .stream()
                                                                            .limit(this_context.req.getMax_count())
                                                                            .collect(Collectors.toCollection(TreeSet::new));
                                                                    return Boolean.TRUE;
                                                                }
                                                                else{
                                                                    return Boolean.FALSE;
                                                                }
                                                            }
                                                            else{
                                                                data.getDatas().add(new_data);
                                                                return Boolean.TRUE;
                                                            }
                                                            */
                                                        },
                                                        Duration.ofSeconds(1)
                                                )
                                                .thenApply(o->{return Boolean.class.cast(o);})

                                );
                })
                .doOnNext(res->{
                    if(res){
                        this_context.actor_ref.tell(
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
