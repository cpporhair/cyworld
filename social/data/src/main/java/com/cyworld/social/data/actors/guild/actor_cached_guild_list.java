package com.cyworld.social.data.actors.guild;

import akka.actor.*;
import com.cyworld.social.data.actors.common.actor_namespace;
import com.cyworld.social.data.entity.guild.cached_guild_list;
import com.cyworld.social.data.entity.guild.guild;
import lombok.Builder;
import lombok.Data;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;

import static akka.pattern.Patterns.ask;

public class actor_cached_guild_list extends AbstractActor {

    @Data
    @Builder
    public static class update_members_count{
        String guild_id;
        int max;
        int cur;
    }
    @Data
    @Builder
    public static class drop_one{
        String guild_id;
    }
    @Data
    @Builder
    public static class update_one{
        String guild_id;
    }
    public static interface msg_readonly_inject_ask<R>{
        R apply(cached_guild_list data);
    }
    public actor_cached_guild_list(){
        System.out.println("actor_cached_guild_list started");
        System.out.println(self().path().toString());
        //self().tell("update",self());
    }
    private static Props props() {
        return Props.create(actor_cached_guild_list.class);
    }

    private cached_guild_list data=new cached_guild_list();
    @Override
    public Receive createReceive() {
        return receiveBuilder()
                .matchEquals("update",msg->{
                    update();
                    context().system().scheduler()
                            .scheduleOnce(
                                    Duration.ofSeconds(10*60),
                                    self(),
                                    "update",
                                    context().system().dispatcher(),
                                    self()
                            );
                })
                .match(drop_one.class,msg->{
                    data.cahce.removeIf(e->{
                        return e.getId().equals(msg.guild_id);
                    });
                })
                .match(update_one.class,msg->{
                    actor_guild.find_or_load(msg.guild_id,getContext().getSystem())
                            .flatMap(ref->{
                                return Mono.fromCompletionStage
                                        (
                                                ask
                                                        (
                                                                ref,
                                                                (actor_guild.msg_readonly_inject_ask<guild, cached_guild_list.cached_guild_data>) data->{
                                                                    cached_guild_list.cached_guild_data res=new cached_guild_list.cached_guild_data();
                                                                    res.setId(data.id);
                                                                    res.setName(data.name);
                                                                    res.setLanguage(data.language);
                                                                    res.setDomain_id(data.domain_id);
                                                                    res.setPower(data.power);
                                                                    res.setOpening(data.opening);
                                                                    res.setMax_member_count(data.max_member_count);
                                                                    res.setCur_member_count(data.members.size());
                                                                    res.setLevel_for_join(data.level_for_join);
                                                                    return res;
                                                                },
                                                                Duration.ofSeconds(1)
                                                        )
                                                        .thenApply(cached_guild_list.cached_guild_data.class::cast)
                                        )
                                        .doOnNext(new_data->{
                                            if(new_data.isOpening())
                                                self().tell(new_data,ActorRef.noSender());
                                        });
                            })
                            .subscribe();
                })
                .match(update_members_count.class,msg->{
                    this.data.cahce.stream().forEach(e->{
                        if(!e.getId().equals(msg.guild_id))
                            return;
                        e.setMax_member_count(msg.max);
                        e.setCur_member_count(msg.cur);
                    });
                })
                .match(msg_readonly_inject_ask.class,msg->{
                    sender().tell(msg.apply(data), self());
                })
                .match(cached_guild_list.cached_guild_data.class, msg->{
                    this.data.cahce.removeIf(e->e.getId().equals(msg.getId()));
                    this.data.cahce.add(msg);
                    System.out.println(this.data.cahce.size()+"AAAAAAAAAAAAAAAA"+msg);
                })
                .match(cached_guild_list.class,msg->{
                    this.data=msg;
                })
                .build();
    }
    private void update(){
        actor_namespace.find_or_build("guildmanager",getContext().getSystem())
                .flatMap(manager_ref->{
                    return Mono.fromCompletionStage
                            (
                                    ask
                                            (
                                                    manager_ref,
                                                    (actor_namespace.msg_join<Iterable<ActorRef>>) actor_namespace_obj->{
                                                        return actor_namespace_obj.getContext().getChildren();
                                                    },
                                                    Duration.ofSeconds(1)
                                            )
                                            .thenApply(iter->{
                                                Iterable<ActorRef> i=Iterable.class.cast(iter);
                                                return i;
                                            })
                            )
                            .flatMapMany(iter->{
                                return Flux.fromIterable(iter);
                            })
                            .flatMap(ref->{
                                return Mono.fromCompletionStage
                                        (
                                                ask
                                                        (
                                                                ref,
                                                                (actor_guild.msg_readonly_inject_ask<guild, cached_guild_list.cached_guild_data>) data->{
                                                                    cached_guild_list.cached_guild_data res=new cached_guild_list.cached_guild_data();
                                                                    res.setId(data.id);
                                                                    res.setName(data.name);
                                                                    res.setLanguage(data.language);
                                                                    res.setDomain_id(data.domain_id);
                                                                    res.setPower(data.power);
                                                                    res.setOpening(data.opening);
                                                                    res.setMax_member_count(data.max_member_count);
                                                                    res.setCur_member_count(data.members.size());
                                                                    res.setLevel_for_join(data.level_for_join);
                                                                    return res;
                                                                },
                                                                Duration.ofSeconds(1)
                                                        )
                                                        .thenApply(cached_guild_list.cached_guild_data.class::cast)
                                        );
                            })
                            .reduceWith(()->new cached_guild_list(),(a,e)->{
                                if(e.isOpening())
                                    a.cahce.add(e);
                                return a;
                            })
                            .doOnNext(cache->{
                                self().tell(cache,self());
                            });
                })
                .subscribe();
    }

    public static Mono<ActorRef> find_or_build(ActorSystem sys) {
        return Mono.just(sys.actorSelection("/user/".concat("cached_guild_list")))
                .flatMap(actorSelection -> {
                    return Mono.fromCompletionStage(actorSelection.resolveOne(Duration.ofSeconds(1)));
                })
                .onErrorResume(e1->{
                    if(e1 instanceof ActorNotFound)
                        return Mono
                                .just(sys.actorOf(actor_cached_guild_list.props(),"cached_guild_list"))
                                .onErrorResume(e2->{
                                    if(e2 instanceof InvalidActorNameException){
                                        if(e2.getMessage().contains("is not unique!"))
                                            return find_or_build(sys);
                                    }
                                    return Mono.error(e2);
                                });
                    else
                        return Mono.error(e1);
                })
                ;//.retry();
    }
}
