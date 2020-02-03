package com.cyworld.social.data.service.guild;

import akka.actor.ActorRef;
import com.cyworld.social.data.DataApplication;
import com.cyworld.social.data.actors.akka_system;
import com.cyworld.social.data.actors.guild.actor_cached_guild_list;
import com.cyworld.social.data.actors.guild.actor_guild;
import com.cyworld.social.data.entity.guild.cached_guild_list;
import com.cyworld.social.data.entity.guild.guild;
import com.cyworld.social.data.entity.guild.guild_member;
import com.cyworld.social.utils.request.guild.req_get_guild_list;
import com.cyworld.social.utils.response.guild.res_get_guild_list;
import com.cyworld.social.utils.response.guild.res_guild_info_simple;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.List;
import java.util.stream.Collectors;

import static akka.pattern.Patterns.ask;

@Component
public class srv_get_guild_list {
    private static final Logger logger = LoggerFactory.getLogger(DataApplication.class.getName());
    @Autowired
    akka_system akka;

    private class context{
        req_get_guild_list req;
        ActorRef cache_ref=null;
    }

    public Mono serv(ServerRequest request){
        context this_context=new context();
        return request.bodyToMono(req_get_guild_list.class)
                .doOnNext(req->{
                    this_context.req=req;
                })
                .flatMap(req->{
                    return actor_cached_guild_list.find_or_build(akka.actor_system);
                })
                .flatMap(ref->{
                    if(ref.isTerminated())
                        return Mono.error(new Throwable("cant find guild.manager"));
                    this_context.cache_ref=ref;
                    return Mono.fromCompletionStage
                            (
                                    ask
                                            (
                                                    this_context.cache_ref,
                                                    (actor_cached_guild_list.msg_readonly_inject_ask<res_get_guild_list>) actor_namespace_obj->{
                                                        res_get_guild_list res=new res_get_guild_list();
                                                        try{
                                                            List<cached_guild_list.cached_guild_data> l1=actor_namespace_obj.cahce.stream()
                                                                    .filter(e->{
                                                                        if(this_context.req.getName()!=null)
                                                                            return e.getName().contains(this_context.req.getName());
                                                                        else
                                                                            return true;
                                                                    })
                                                                    .filter(e->{
                                                                        if(this_context.req.getDomain_id()!=null && e.getDomain_id()!=null)
                                                                            return e.getDomain_id().toUpperCase().equals(this_context.req.getDomain_id().toUpperCase());
                                                                        else
                                                                            return true;
                                                                    })
                                                                    .filter(e->{
                                                                        if(this_context.req.getLanguage()!=null && e.getLanguage()!=null)
                                                                            return e.getLanguage().toUpperCase().equals(this_context.req.getLanguage().toUpperCase());
                                                                        else
                                                                            return true;
                                                                    })
                                                                    .filter(e->{
                                                                        if(!this_context.req.isRecommend())
                                                                            return true;
                                                                        else if(e.getLevel_for_join()>this_context.req.getLevel())
                                                                            return false;
                                                                        else if(e.getMax_member_count()<=e.getCur_member_count())
                                                                            return false;
                                                                        else
                                                                            return true;
                                                                    })
                                                                    .collect(Collectors.toList());
                                                            res.setMax_count(l1.size());
                                                            List l2=l1.stream()
                                                                    .skip(this_context.req.getSkip())
                                                                    .limit(this_context.req.getTake())
                                                                    .map(e->{
                                                                        res_guild_info_simple info=new res_guild_info_simple();
                                                                        info.setId(e.getId());
                                                                        info.setName(e.getName());
                                                                        info.setPower(e.getPower());
                                                                        return info;
                                                                    })
                                                                    .collect(Collectors.toList());
                                                            res.setList(l2);
                                                            return res;
                                                        }
                                                        catch (Exception e){
                                                            logger.error(e.getMessage());
                                                            return res;
                                                        }

                                                    },
                                                    Duration.ofSeconds(1)
                                            )
                                            .thenApply(res_get_guild_list.class::cast)
                            );
                })
                .flatMap(the_res_get_guild_list -> {
                    return Flux.fromIterable(the_res_get_guild_list.getList())
                            .flatMap(info_simple -> {
                                return actor_guild.find_or_load(info_simple.getId(),akka.actor_system)
                                        .flatMap(ref->{
                                            if(ref.isTerminated())
                                                return Mono.empty();
                                            else
                                                return Mono.fromCompletionStage
                                                        (
                                                                ask
                                                                        (
                                                                                ref,
                                                                                (actor_guild.msg_readonly_inject_ask<guild, Boolean>) data->{

                                                                                    guild_member owner=data.find_owner();

                                                                                    info_simple.setId(data.id);
                                                                                    info_simple.setExp(data.exp);
                                                                                    info_simple.setFlag(data.flag);
                                                                                    info_simple.setMax_count(data.max_member_count);
                                                                                    info_simple.setMember_count(data.members.size());
                                                                                    info_simple.setName(data.name);
                                                                                    info_simple.setOwner_id(owner.getPlayer_id());
                                                                                    info_simple.setOwner_namespace(owner.getPlayer_namespace());
                                                                                    info_simple.setNeed_apply(data.need_apply_when_join);
                                                                                    info_simple.setLevel_for_join(data.level_for_join);
                                                                                    info_simple.setAsked
                                                                                            (
                                                                                                    data.ask_list.stream().anyMatch(e->{
                                                                                                        return e.getSrc_namespace().equals(this_context.req.getPlayer_namespace())
                                                                                                                &&
                                                                                                                e.getSrc_id().equals(this_context.req.getPlayer_id());
                                                                                                    })
                                                                                            );
                                                                                    return Boolean.TRUE;
                                                                                },
                                                                                Duration.ofSeconds(1)
                                                                        )
                                                                        .thenApply(Boolean.class::cast)
                                                        );
                                        });
                            })
                            .count()
                            .map(c->the_res_get_guild_list);
                })
                .flatMap(the_res_get_guild_list->{
                    return ServerResponse.ok().contentType(MediaType.APPLICATION_JSON).body(BodyInserters.fromObject(the_res_get_guild_list));
                });
    }
}
