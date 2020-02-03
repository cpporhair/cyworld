package com.cyworld.social.data.service.friends;

import akka.actor.ActorSystem;
import com.alibaba.fastjson.JSON;
import com.cyworld.social.data.actors.friends.actor_friends_data;
import com.cyworld.social.data.entity.friends.player_friends_data;
import com.cyworld.social.utils.response.friend.res_one_get_help_data;
import lombok.Builder;
import lombok.Data;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.List;

import static akka.pattern.Patterns.ask;

public class act_foreach_friend {
    public static <T> Flux<T> foreach_friend_inject(String player_full_id,
                                             Iterable<String[]> friends,
                                             ActorSystem sys,
                                             actor_friends_data.msg_inject_ask<player_friends_data,T> msg){
        return Flux.fromIterable(friends)
                .flatMap(s->{
                    return actor_friends_data.find_or_build(s[0],s[1],sys);
                })
                .flatMap(ref->{
                    if(ref.isTerminated())
                        return Mono.empty();
                    else
                        return Mono.fromCompletionStage(ask(ref,msg,Duration.ofSeconds(1)).thenApply(o->(T)o));
                });
    }
    public static Mono<List<String>> get_help_data(String player_full_id, Iterable<String[]> friends, ActorSystem sys){
        return foreach_friend_inject
                (
                        player_full_id,
                        friends,
                        sys,
                        (actor_friends_data.msg_inject_ask<player_friends_data, res_one_get_help_data>) data->{
                            data.update_current_help_data();
                            if(data.getCurrent_help_data()==null)
                                return res_one_get_help_data.builder().build();
                            if(data.getCurrent_help_data().getHelp_amount()<=0)
                                return res_one_get_help_data.builder().build();
                            if(data.getCurrent_help_data().getDonors().stream().anyMatch(o->o.equals(player_full_id)))
                                return res_one_get_help_data.builder().build();
                            return res_one_get_help_data.builder()
                                    .friend_namespace(data.getNamespace())
                                    .frined_id(data.get_id())
                                    .help_amount(data.getCurrent_help_data().getHelp_amount())
                                    .help_type(data.getCurrent_help_data().getHelp_type())
                                    .build();
                        }
                )
                .filter(o->o.getFrined_id()!=null)
                .map(o-> JSON.toJSONString(o))
                .collectList();
    }
}
