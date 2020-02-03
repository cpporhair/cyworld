package com.cyworld.social.data.actors.guild;

import akka.actor.ActorNotFound;
import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Props;
import com.cyworld.social.data.actors.common.actor_entity;
import com.cyworld.social.data.actors.common.actor_namespace;
import com.cyworld.social.data.entity.guild.guild_member;
import com.cyworld.social.data.entity.guild.player_guild_data;
import com.cyworld.social.data.mongodb.db_context;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import reactor.core.publisher.Mono;
import scala.Tuple2;

import java.time.Duration;

import static akka.pattern.Patterns.ask;

public class actor_player_guild_data extends actor_entity<player_guild_data> {
    protected actor_player_guild_data(player_guild_data data,boolean isnew) {
        super(data,isnew);
    }

    @Override
    protected Mono<player_guild_data> save_mono(player_guild_data data) {
        return db_context.mongo_templates.get("guild").getTemplate().save(data,data.getNamespace());
    }

    private static Props props(player_guild_data data,boolean isnew) {
        return Props.create(actor_player_guild_data.class, data,isnew);
    }

    public static Mono<ActorRef> load(String namespace, String id, ActorSystem sys) {
        return db_context.mongo_templates.get("guild").getTemplate()
                .findOne
                        (
                                Query.query(Criteria.where("_id").is(id)),
                                player_guild_data.class,
                                namespace//compute_collections_name(namespace)
                        )
                .flatMap(data -> {
                    return actor_namespace.find_or_build("guild",data.getNamespace(), sys)
                            .map(ref->{
                                return new Tuple2<player_guild_data,ActorRef>(data,ref);
                            })
                            .flatMap(player_guild_dataActorRefTuple2 -> {
                                return Mono.fromCompletionStage(
                                        ask
                                                (
                                                        player_guild_dataActorRefTuple2._2,
                                                        actor_namespace.create_child.builder()
                                                                .name(data.getId()).props(actor_player_guild_data.props(data,false)).build(),
                                                        Duration.ofSeconds(1)
                                                )
                                                .thenApply(ActorRef.class::cast)
                                );
                            });
                });
    }
    public static Mono<ActorRef> create(player_guild_data data, ActorSystem sys){
        return actor_namespace.find_or_build("guild",data.getNamespace(), sys)
                .map(ref->{
                    return new Tuple2<player_guild_data,ActorRef>(data,ref);
                })
                .flatMap(player_guild_dataActorRefTuple2 -> {
                    return Mono.fromCompletionStage(
                            ask
                                    (
                                            player_guild_dataActorRefTuple2._2,
                                            actor_namespace.create_child.builder()
                                                    .name(data.getId()).props(actor_player_guild_data.props(data,true)).build(),
                                            Duration.ofSeconds(1)
                                    )
                                    .thenApply(ActorRef.class::cast)
                    );
                });
    }
    public static Mono<ActorRef> find_or_load(String namespace, String id, ActorSystem sys) {
        return Mono.just(sys.actorSelection(compute_search_path(namespace, id)))
                .flatMap(actorSelection -> {
                    return Mono.fromCompletionStage(actorSelection.resolveOne(Duration.ofSeconds(1)));
                })
                .onErrorResume(e->{
                    if(e instanceof ActorNotFound)
                        return load(namespace, id, sys);
                    else
                        return Mono.error(e);
                })
                ;//.retry();
    }
    public static String compute_search_path(String namespace, String player_id) {
        return "/user/guild.namespace.".concat(namespace).concat("/").concat(player_id);
    }
}
