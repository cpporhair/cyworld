package com.cyworld.social.data.actors.mail;

import akka.actor.ActorNotFound;
import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Props;
import com.cyworld.social.data.actors.common.actor_entity;
import com.cyworld.social.data.actors.common.actor_namespace;
import com.cyworld.social.data.entity.mail.mailbox;
import com.cyworld.social.data.mongodb.db_context;
import com.cyworld.social.data.utils.static_define;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import reactor.core.publisher.Mono;
import scala.Tuple2;

import java.time.Duration;

import static akka.pattern.Patterns.ask;

public class actor_mailbox extends actor_entity<mailbox> {
    private actor_mailbox(mailbox data,boolean isnew) {
        super(data,isnew);
    }

    @Override
    protected Mono<mailbox> save_mono(mailbox data) {
        return db_context.mongo_templates.get("mail").getTemplate().save(data,data.getNamespace());
    }

    private static Props props(mailbox data,boolean isnew) {
        return Props.create(actor_mailbox.class, data, isnew);
    }

    public static Mono<ActorRef> create(mailbox box, ActorSystem sys) {
        return actor_namespace.find_or_build("mail",box.getNamespace(), sys)
                .map(ref->{
                    return new Tuple2<mailbox,ActorRef>(box,ref);
                })
                .flatMap(player_guild_dataActorRefTuple2 -> {
                    return Mono.fromCompletionStage(
                            ask
                                    (
                                            player_guild_dataActorRefTuple2._2,
                                            actor_namespace.create_child.builder()
                                                    .name(box.get_id()).props(props(box,true)).build(),
                                            Duration.ofSeconds(1)
                                    )
                                    .thenApply(ActorRef.class::cast)
                    );
                });
    }

    public static Mono<ActorRef> load(String namespace, String id, ActorSystem sys) {
        return db_context.mongo_templates.get("mail").getTemplate()
                .findOne
                        (
                                Query.query(Criteria.where("_id").is(id)),
                                mailbox.class,
                                namespace
                        )
                .switchIfEmpty(Mono.error(new static_define.throwable_empty_actor("cant find in DB")))
                .flatMap(box -> {
                    return actor_namespace.find_or_build("mail",box.getNamespace(), sys)
                            .map(ref->{
                                return new Tuple2<mailbox,ActorRef>(box,ref);
                            })
                            .flatMap(player_guild_dataActorRefTuple2 -> {
                                return Mono.fromCompletionStage(
                                        ask
                                                (
                                                        player_guild_dataActorRefTuple2._2,
                                                        actor_namespace.create_child.builder()
                                                                .name(box.get_id()).props(props(box,false)).build(),
                                                        Duration.ofSeconds(1)
                                                )
                                                .thenApply(ActorRef.class::cast)
                                );
                            });
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

    public static String compute_search_path(String namespace, String mailbox_id) {
        return "/user/mail.namespace.".concat(namespace).concat("/").concat(mailbox_id);
    }

    public static String compute_collections_name(String namespace) {
        return "namespace.".concat(namespace);
    }

    public static void unsubscribe(ActorRef ref,String group_id){
        ref.tell(
                (actor_mailbox.msg_inject_tell<mailbox>)box->{
                    box.getMail_groups().removeIf(e->e.getMailgroup_ID().equals(group_id));
                },
                ActorRef.noSender()
        );
    }
}
