package com.cyworld.social.data.service.guild;

import akka.actor.ActorRef;
import com.cyworld.social.data.actors.akka_system;
import com.cyworld.social.data.actors.guild.actor_cached_guild_list;
import com.cyworld.social.data.actors.guild.actor_guild;
import com.cyworld.social.data.actors.public_message_define;
import com.cyworld.social.data.entity.guild.cached_guild_list;
import com.cyworld.social.data.entity.guild.guild;
import com.cyworld.social.utils.request.guild.req_change_domain;
import com.cyworld.social.utils.request.guild.req_get_guild_list;
import org.springframework.beans.factory.annotation.Autowired;
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
public class srv_change_domain {
    @Autowired
    akka_system akka;

    private class context{
        req_change_domain req;
        ActorRef cache_ref=null;
    }

    private Mono change_all(req_change_domain req){
        return actor_cached_guild_list.find_or_build(akka.actor_system)
                .flatMap(ref_cached_guild_list->{
                    return Mono.fromCompletionStage
                            (
                                    ask
                                            (
                                                    ref_cached_guild_list,
                                                    (actor_cached_guild_list.msg_readonly_inject_ask<List<String>>) actor_namespace_obj->{
                                                        List<String> res=actor_namespace_obj.cahce.stream()
                                                                .filter(e->{
                                                                    if(req.getNew_domain()!=null)
                                                                        return (req.getChange_id().toUpperCase().equals(e.getDomain_id().toUpperCase()));
                                                                    else
                                                                        return true;
                                                                })
                                                                .map(e->{
                                                                    return e.getId();
                                                                })
                                                                .collect(Collectors.toList());
                                                        return res;
                                                    },
                                                    Duration.ofSeconds(1)
                                            )
                                            .thenApply(List.class::cast)
                            )
                            .flatMapMany(l->{
                                List<String> l1=l;
                                return Flux.fromIterable(l1);
                            })
                            .doOnNext(guild_id->{
                                actor_guild.find_or_load(guild_id,akka.actor_system)
                                        .doOnNext(ref->{
                                            if(ref.isTerminated())
                                                return;
                                            ref.tell(
                                                    (actor_guild.msg_inject_tell<guild>) data->{
                                                        data.domain_id=req.getNew_domain();
                                                    },
                                                    ActorRef.noSender()
                                            );
                                            ref.tell(public_message_define.SAVE_SIGNAL,ActorRef.noSender());
                                            ref_cached_guild_list.tell(
                                                    actor_cached_guild_list.update_one
                                                            .builder()
                                                            .guild_id(guild_id)
                                                            .build(),
                                                    ActorRef.noSender()
                                            );
                                        });
                            })
                            .all(o->true)
                            .flatMap(unused->{
                                return ServerResponse.ok().body(BodyInserters.fromObject("OK"));
                            });
                });
    }
    private Mono change_one(String guild_id,String new_domain){
        return actor_guild.find_or_load(guild_id,akka.actor_system)
                .flatMap(ref->{
                    if(ref.isTerminated())
                        return Mono.error(new Throwable("cant find guild actor"));
                    ref.tell(
                            (actor_guild.msg_inject_tell<guild>) data->{
                                data.domain_id=new_domain;
                            },
                            ActorRef.noSender()
                    );
                    ref.tell(public_message_define.SAVE_SIGNAL,ActorRef.noSender());
                    return actor_cached_guild_list.find_or_build(akka.actor_system)
                            .doOnNext(ref_cached_guild_list->{
                                ref_cached_guild_list.tell(
                                        actor_cached_guild_list.update_one
                                                .builder()
                                                .guild_id(guild_id)
                                                .build(),
                                        ActorRef.noSender()
                                );
                            });
                })
                .flatMap(unused->{
                    return ServerResponse.ok().body(BodyInserters.fromObject("OK"));
                });
    }
    public Mono serv(ServerRequest request){
        return request.bodyToMono(req_change_domain.class)
                .flatMap(req->{
                    if(req.getChange_type()==0)
                        return change_all(req);
                    else
                        return change_one(req.getChange_id(),req.getNew_domain());
                });
    }
}
