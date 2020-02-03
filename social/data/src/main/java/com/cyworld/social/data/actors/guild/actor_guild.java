package com.cyworld.social.data.actors.guild;

import akka.actor.*;
import com.cyworld.social.data.actors.common.actor_entity;
import com.cyworld.social.data.actors.common.actor_namespace;
import com.cyworld.social.data.entity.guild.guild;
import com.cyworld.social.data.mongodb.db_context;
import com.cyworld.social.data.utils.static_define;
import com.google.common.primitives.UnsignedLong;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import reactor.core.publisher.Mono;
import scala.Tuple2;

import java.time.Duration;

import static akka.pattern.Patterns.ask;

public class actor_guild extends actor_entity<guild> {

    protected actor_guild(guild data,boolean isnew) {
        super(data,isnew);
    }

    @Override
    protected boolean auto_close(){
        return false;
    }

    @Override
    protected Mono<guild> save_mono(guild data) {
        return db_context.mongo_templates.get("guild").getTemplate().save(data,"guilds");
    }

    private static Props props(guild data,boolean isnew) {
        return Props.create(actor_guild.class, data, isnew);
    }
    public static Mono<ActorRef> load(String id, ActorSystem sys) {
        return db_context.mongo_templates.get("guild").getTemplate()
                .findOne
                        (
                                Query.query(Criteria.where("id").is(id)),
                                guild.class,
                                "guilds"
                        )
                .switchIfEmpty(Mono.error(new static_define.throwable_empty_actor("cant find guild")))
                .flatMap(data -> {
                    if(data.domain_id==null)
                        data.domain_id="0";
                    if(!data.opening)
                        return Mono.error(new static_define.throwable_empty_actor("cant find guild"));
                    else
                        return create(data,sys,false);
                });
    }
    public static Mono<ActorRef> create(guild data, ActorSystem sys,boolean isnew){
        return actor_namespace.find_or_build("guildmanager", sys)
                .map(ref->{
                    return new Tuple2<guild,ActorRef>(data,ref);
                })
                .flatMap(tuple2->{
                    return Mono.fromCompletionStage(
                            ask
                                    (
                                            tuple2._2,
                                            actor_namespace.create_child
                                                    .builder()
                                                    .name(data.id)
                                                    .props(actor_guild.props(data,isnew))
                                                    .build(),
                                            Duration.ofSeconds(1)
                                    )
                                    .thenApply(ActorRef.class::cast)
                    );
                });
    }
    public static Mono<ActorRef> create(guild data, ActorSystem sys){
        return create(data,sys,true);
    }
    public static Mono<ActorRef> find_or_load(String id, ActorSystem sys) {
        return Mono.just(sys.actorSelection(compute_search_path(id)))
                .flatMap(actorSelection -> {
                    return Mono.fromCompletionStage(actorSelection.resolveOne(Duration.ofSeconds(100)));
                })
                .onErrorResume(e->{
                    if(e instanceof ActorNotFound)
                        return load(id, sys);
                    else
                        return Mono.error(e);
                });
                //.retry();
    }
    public static Mono<Boolean> has_actor(String id,ActorSystem sys){
        return Mono.just(sys.actorSelection(compute_search_path(id)))
                .flatMap(actorSelection -> {
                    return Mono.fromCompletionStage(actorSelection.resolveOne(Duration.ofSeconds(100)));
                })
                .map(ref->true)
                .onErrorResume(e->{
                    if(e instanceof ActorNotFound)
                        return Mono.just(false);
                    else
                        return Mono.error(e);
                });
    }
    public static boolean has_same_guild_actor(String id,ActorSystem sys){
        return !sys.actorFor(compute_search_path(id)).isTerminated();
    }
    public static String compute_search_path(String id) {
        return "/user/guildmanager/".concat(id);
    }
}
