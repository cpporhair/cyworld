package com.cyworld.social.data.actors.friends;

import akka.actor.*;
import com.cyworld.social.data.actors.common.actor_entity;
import com.cyworld.social.data.actors.common.actor_namespace;
import com.cyworld.social.data.entity.friends.player_friends_data;
import com.cyworld.social.data.mongodb.db_context;
import com.cyworld.social.data.utils.static_define;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import reactor.core.publisher.Mono;
import scala.Tuple2;

import java.time.Duration;

import static akka.pattern.Patterns.ask;

public class actor_friends_data extends actor_entity<player_friends_data> {
    private actor_friends_data(player_friends_data data,boolean is_new) {
        super(data,is_new);
    }

    @Override
    protected Mono<player_friends_data> save_mono(player_friends_data data) {
        return db_context.mongo_templates.get("friend").getTemplate().save(data,data.getNamespace());
    }

    private static Props props(player_friends_data data,boolean is_new) {
        return Props.create(actor_friends_data.class, data,is_new);
    }


    public static Mono<ActorRef> load(String namespace, String id, ActorSystem sys) {
        return db_context.mongo_templates.get("friend").getTemplate()
                .findOne
                        (
                                Query.query(Criteria.where("_id").is(id)),
                                player_friends_data.class,
                                namespace//compute_collections_name(namespace)
                        )
                .switchIfEmpty(Mono.error(new static_define.throwable_empty_actor("cant find in DB")))
                .flatMap(data->{
                    return actor_namespace.find_or_build("friend",namespace, sys)
                            .map(ref->{
                                return new Tuple2<player_friends_data,ActorRef>(data,ref);
                            })
                            .flatMap(tuple2 -> {
                                return Mono.fromCompletionStage(
                                        ask
                                                (
                                                        tuple2._2,
                                                        actor_namespace.create_child.builder()
                                                                .name(tuple2._1.get_id())
                                                                .props(actor_friends_data.props(tuple2._1,false)).build(),
                                                        Duration.ofSeconds(1)
                                                )
                                                .thenApply(ActorRef.class::cast)
                                );
                            });
                });
    }
    public static Mono<ActorRef> create(player_friends_data data, ActorSystem sys){
        return actor_namespace.find_or_build("friend",data.getNamespace(), sys)
                .map(ref->{
                    return new Tuple2<player_friends_data,ActorRef>(data,ref);
                })
                .flatMap(tuple2->{
                    return Mono.fromCompletionStage(
                            ask
                                    (
                                            tuple2._2,
                                            actor_namespace.create_child.builder().name(data.get_id()).props(actor_friends_data.props(data,true)).build(),
                                            Duration.ofSeconds(1)
                                    )
                                    .thenApply(ActorRef.class::cast));
                });
    }
    public static Mono<ActorRef> find_or_build(String namespace, String id, ActorSystem sys) {
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
    //public static String compute_collections_name(String namespace) {
    //    return "namespace.".concat(namespace);
    //}
    public static String compute_search_path(String namespace, String player_id) {
        return "/user/friend.namespace.".concat(namespace).concat("/").concat(player_id);
    }
}
