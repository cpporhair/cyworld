package com.cyworld.social.data.actors.mail;

import akka.actor.*;
import com.cyworld.social.data.actors.common.actor_entity;
import com.cyworld.social.data.actors.common.actor_namespace;
import com.cyworld.social.data.entity.mail.mailgroup;
import com.cyworld.social.data.mongodb.db_context;
import com.cyworld.social.data.utils.static_define;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import reactor.core.publisher.Mono;
import scala.Tuple2;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import static akka.pattern.Patterns.ask;

public class actor_mailgroup extends actor_entity<mailgroup> {
    private static final Logger logger = LoggerFactory.getLogger(actor_mailgroup.class.getName());

    public actor_mailgroup(mailgroup group,boolean isnew) {
        super(group,isnew);
    }

    @Override
    protected Mono<mailgroup> save_mono(mailgroup data) {
        return db_context.mongo_templates.get("mail").getTemplate().save(data,"mailgroup");
    }

    private static Props props(mailgroup group,boolean isnew) {
        return Props.create(actor_mailgroup.class, group, isnew);
    }

    public static Mono<ActorRef> create(mailgroup group, ActorSystem system) {
        return actor_namespace.find_or_build("mailgroups", system)
                .map(ref->{
                    return new Tuple2<mailgroup,ActorRef>(group,ref);
                })
                .flatMap(player_guild_dataActorRefTuple2 -> {
                    return Mono.fromCompletionStage(
                            ask
                                    (
                                            player_guild_dataActorRefTuple2._2,
                                            actor_namespace.create_child
                                                    .builder()
                                                    .name(compute_create_name(group.getId()))
                                                    .props(actor_mailgroup.props(group,true)).build(),
                                            Duration.ofSeconds(1)
                                    )
                                    .thenApply(ActorRef.class::cast)
                    );
                });
    }

    public static Mono<ActorRef> load(String id, ActorSystem system) {
        return db_context.mongo_templates.get("mail").getTemplate().findOne
                (
                        Query.query(Criteria.where("_id").is(id)),
                        mailgroup.class,
                        "mailgroup"
                )
                .switchIfEmpty(Mono.error(new static_define.throwable_empty_actor("cant find mailgroup id")))
                .flatMap(grp -> {
                    if(!grp.isOpening())
                        return Mono.error(new static_define.throwable_empty_actor("cant find mailgroup id"));
                    else
                        return actor_namespace.find_or_build("mailgroups", system)
                                .map(ref->{
                                    return new Tuple2<mailgroup,ActorRef>(grp,ref);
                                })
                                .flatMap(player_guild_dataActorRefTuple2 -> {
                                    return Mono.fromCompletionStage(
                                            ask
                                                    (
                                                            player_guild_dataActorRefTuple2._2,
                                                            actor_namespace.create_child
                                                                    .builder()
                                                                    .name(compute_create_name(grp.getId()))
                                                                    .props(actor_mailgroup.props(grp,false)).build(),
                                                            Duration.ofSeconds(1)
                                                    )
                                                    .thenApply(ActorRef.class::cast)
                                    );
                                });
                        //return Mono.just(system.actorOf(actor_mailgroup.props(grp,false), compute_create_name(grp.getId())));
                });
    }

    public static Mono<ActorRef> find_or_build(String id, ActorSystem system) {
        return Mono.just(system.actorSelection(compute_search_path(id)))
                .flatMap(actorSelection -> {
                    return Mono.fromCompletionStage(actorSelection.resolveOne(Duration.ofSeconds(1)));
                })
                .onErrorResume(e->{
                    if(e instanceof ActorNotFound)
                        return load(id, system);
                    else
                        return Mono.error(e);
                })
                ;//.retry();
    }

    public static String compute_search_path(String id) {
        return "/user/mailgroups/mailgroup.".concat(id);
    }

    public static String compute_create_name(String id) {
        return "mailgroup.".concat(id);
    }
}
