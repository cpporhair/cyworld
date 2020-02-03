package com.cyworld.social.data.service.guild;

import akka.actor.ActorRef;
import com.cyworld.social.data.actors.akka_system;
import com.cyworld.social.data.actors.guild.actor_cached_guild_list;
import com.cyworld.social.data.actors.guild.actor_guild;
import com.cyworld.social.data.actors.guild.actor_player_guild_data;
import com.cyworld.social.data.actors.public_message_define;
import com.cyworld.social.data.entity.guild.guild;
import com.cyworld.social.data.entity.guild.guild_member;
import com.cyworld.social.data.entity.guild.player_guild_data;
import com.cyworld.social.utils.request.guild.req_drop_guild;
import com.cyworld.social.utils.response.guild.res_drop_guild;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

import java.time.Duration;
import static akka.pattern.Patterns.ask;
@Component
public class srv_drop_guild {
    @Autowired
    akka_system akka;

    private class context{
        req_drop_guild req;
        ActorRef target_ref=null;
        ActorRef guild_ref=null;
    }

    public Mono serv(ServerRequest request){
        context this_context=new context();
        return request.bodyToMono(req_drop_guild.class)
                .doOnNext(req->{
                    this_context.req=req;
                })
                .flatMap(req->{
                    return actor_player_guild_data.find_or_load(req.getPlayer_namespace(),req.getPlayer_id(),akka.actor_system);
                })
                .flatMap(ref->{
                    if(ref.isTerminated())
                        return Mono.error(new Throwable("cant find target actor"));
                    this_context.target_ref=ref;
                    return actor_guild.find_or_load(this_context.req.getGuild_id(),akka.actor_system);
                })
                .flatMap(ref->{
                    if(ref.isTerminated())
                        return Mono.error(new Throwable("cant find guild actor"));
                    this_context.guild_ref=ref;
                    return Mono.fromCompletionStage
                            (
                                    ask
                                            (
                                                    this_context.guild_ref,
                                                    (actor_guild.msg_inject_ask<guild, Integer>) data->{
                                                        if(data.members.size()!=1)
                                                            return res_drop_guild.not_empty;
                                                        guild_member m=data.members.get(0);
                                                        if(!(m.getPlayer_namespace().equals(this_context.req.getPlayer_namespace())&&
                                                                m.getPlayer_id().equals(this_context.req.getPlayer_id())))
                                                            return res_drop_guild.not_owner;
                                                        data.members.clear();
                                                        data.opening=false;
                                                        data.max_member_count=0;
                                                        this_context.guild_ref.tell(public_message_define.SAVE_NOW_SIGNAL,ActorRef.noSender());
                                                        this_context.guild_ref.tell(public_message_define.CLOSE_SIGNAL,ActorRef.noSender());
                                                        return res_drop_guild.result_ok;
                                                    },
                                                    Duration.ofSeconds(1)
                                            )
                                            .thenApply(o->{
                                                Integer res=Integer.class.cast(o);
                                                return res;
                                            })
                            );
                })
                .doOnNext(res->{
                    if(res==res_drop_guild.result_ok){
                        actor_cached_guild_list.find_or_build(akka.actor_system)
                                .doOnNext(ref->{
                                    ref.tell(
                                            actor_cached_guild_list.drop_one
                                                    .builder()
                                                    .guild_id(this_context.req.getGuild_id())
                                                    .build(),
                                            ActorRef.noSender());
                                })
                        .subscribe();

                        this_context.target_ref.tell(
                                (actor_player_guild_data.msg_inject_tell<player_guild_data>)data->{
                                    data.setGuild_id("NULL");
                                },
                                ActorRef.noSender()
                        );
                        this_context.target_ref.tell
                                (
                                        public_message_define.SAVE_SIGNAL,
                                        ActorRef.noSender()
                                );
                    }
                })
                .flatMap(res->{
                    res_drop_guild resDropGuild=new res_drop_guild();
                    resDropGuild.setResult_value(res);
                    return ServerResponse.ok()
                            .contentType(MediaType.APPLICATION_JSON)
                            .body(BodyInserters.fromObject(resDropGuild));
                });
    }
}
