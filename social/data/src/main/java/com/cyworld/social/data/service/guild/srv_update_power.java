package com.cyworld.social.data.service.guild;

import akka.actor.ActorRef;
import com.cyworld.social.data.actors.akka_system;
import com.cyworld.social.data.actors.guild.actor_guild;
import com.cyworld.social.data.entity.guild.guild;
import com.cyworld.social.utils.request.guild.req_update_power;
import com.cyworld.social.utils.request.guild.req_update_power_component;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import static akka.pattern.Patterns.ask;
@Component
public class srv_update_power {
    @Autowired
    akka_system akka;

    private class context{
        req_update_power req;
    }

    public Mono serv(ServerRequest request) {
        context this_context = new context();
        return request.bodyToMono(req_update_power.class)
                .doOnNext(req -> {
                    this_context.req = req;
                })
                .map(req->{
                    HashMap<String, List<req_update_power_component>> hashMap=new HashMap<>();
                    req.getComponent().stream().forEach(e->{
                        List<req_update_power_component> v=hashMap.getOrDefault(e.getGuild_id(),new ArrayList<>());
                        v.add(e);
                        hashMap.put(e.getGuild_id(),v);
                    });
                    return hashMap.entrySet();
                })
                .flatMapMany(set->{
                    return Flux.fromIterable(set);
                })
                .flatMap(entry->{
                    return actor_guild.find_or_load(entry.getKey(),akka.actor_system)
                            .flatMap(ref->{
                                if(ref.isTerminated())
                                    return Mono.empty();
                                ref.tell(
                                        (actor_guild.msg_inject_tell<guild>) data->{
                                            data.update_members_power(entry.getValue());
                                        },
                                        ActorRef.noSender()
                                );
                                return Mono.just(ref);
                            });
                })
                .all(o->true)
                .flatMap(unused->{
                    return ServerResponse.ok().body(BodyInserters.fromObject("OK"));
                });
    }
}
