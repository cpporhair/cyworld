package com.cyworld.social.data.actors.ranks;

import akka.actor.ActorNotFound;
import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Props;
import com.cyworld.social.data.actors.common.actor_entity;
import com.cyworld.social.data.actors.common.actor_namespace;
import com.cyworld.social.data.entity.ranks.ranks;
import com.cyworld.social.data.mongodb.db_context;
import com.cyworld.social.data.utils.static_define;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import reactor.core.publisher.Mono;
import scala.Tuple2;

import java.time.Duration;

import static akka.pattern.Patterns.ask;


public class actor_ranks extends actor_entity<ranks> {
    protected actor_ranks(ranks data,boolean isnew) {
        super(data,isnew);
    }

    @Override
    protected Mono<ranks> save_mono(ranks data) {
        return db_context.mongo_templates.get("ranks").getTemplate().save(data,"ranks");
    }

    private static Props props(ranks data,boolean isnew) {
        return Props.create(actor_ranks.class, data,isnew);
    }
    public static Mono<ActorRef> load(String type, ActorSystem sys) {
        return db_context.mongo_templates.get("ranks").getTemplate()
                .findOne
                        (
                                Query.query(Criteria.where("id").is(type)),
                                ranks.class,
                                compute_collections_name("ranks")
                        )
                .switchIfEmpty(Mono.just(ranks.builder().id(type).build()))
                .flatMap(data -> {
                    return actor_namespace.find_or_build("ranksmanager", sys)
                            .map(ref->{
                                return new Tuple2<ranks,ActorRef>(data,ref);
                            })
                            .flatMap(ranksActorRefTuple2 -> {
                                return Mono.fromCompletionStage(
                                        ask
                                                (
                                                        ranksActorRefTuple2._2,
                                                        actor_namespace.create_child
                                                                .builder()
                                                                .name(data.getId())
                                                                .props(actor_ranks.props(data,false))
                                                                .build(),
                                                        Duration.ofSeconds(1)
                                                )
                                                .thenApply(ActorRef.class::cast)
                                );
                            });

                });
    }

    public static Mono<ActorRef> create(ranks data, ActorSystem sys){
        return actor_namespace.find_or_build("ranksmanager", sys)
                .map(ref->{
                    return new Tuple2<ranks,ActorRef>(data,ref);
                })
                .flatMap(ranksActorRefTuple2 -> {
                    return Mono.fromCompletionStage(
                            ask
                                    (
                                            ranksActorRefTuple2._2,
                                            actor_namespace.create_child
                                                    .builder()
                                                    .name(data.getId())
                                                    .props(actor_ranks.props(data,true))
                                                    .build(),
                                            Duration.ofSeconds(1)
                                    )
                                    .thenApply(ActorRef.class::cast)
                    );
                });
    }
    public static Mono<ActorRef> find_or_build(String type, ActorSystem sys) {
        return Mono.just(sys.actorSelection(compute_search_path(type)))
                .flatMap(actorSelection -> {
                    return Mono.fromCompletionStage(actorSelection.resolveOne(Duration.ofSeconds(1)));
                })
                .onErrorResume(e->{
                    if(e instanceof ActorNotFound)
                        return load(type, sys);
                    else
                        return Mono.error(e);
                })
                ;//.retry();
    }
    public static String compute_collections_name(String type) {
        return type;
    }
    public static String compute_search_path(String type) {
        return "/user/ranksmanager/".concat(type);
    }
}
