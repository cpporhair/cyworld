package com.cyworld.social.data.actors.common;

import akka.actor.*;
import lombok.Builder;
import reactor.core.publisher.Mono;
import scala.Option;

import java.time.Duration;
import java.util.Optional;

public class actor_namespace extends AbstractActor {
    @Override
    public Receive createReceive() {
        return receiveBuilder()
                .match(create_child.class, b -> {
                    Option<ActorRef> op=getContext().child(b.name);
                    if(op.isEmpty())
                        sender().tell(getContext().actorOf(b.props, b.name), self());
                    else
                        sender().tell(op.get(), self());
                })
                .match(msg_join.class,msg->{
                    sender().tell(msg.apply(this), self());
                })
                .build();
    }

    public static Props props() {
        return Props.create(actor_namespace.class);
    }

    /*
    public static ActorRef find_or_create(String type_name, String namespace, ActorSystem sys) {
        return find_or_create(type_name.concat(".namespace.").concat(namespace),sys);
    }

    public static ActorRef find_or_create(String fullname, ActorSystem sys){
        ActorRef namespace_ref = sys.actorFor("/user/".concat(fullname));
        if (namespace_ref.isTerminated()) {
            synchronized (actor_namespace.class) {
                namespace_ref = sys.actorFor("/user/".concat(fullname));
                if (namespace_ref.isTerminated())
                    namespace_ref = sys.actorOf(actor_namespace.props(), fullname);
            }
        }
        return namespace_ref;
    }
    */
    public static Mono<ActorRef> find_or_build(String type_name, String namespace, ActorSystem sys) {
        return find_or_build(type_name.concat(".namespace.").concat(namespace),sys);
    }

    public static Mono<ActorRef> find_or_build(String fullname, ActorSystem sys){
        return Mono.just(sys.actorSelection("/user/".concat(fullname)))
                .flatMap(actorSelection -> {
                    return Mono.fromCompletionStage(actorSelection.resolveOne(Duration.ofSeconds(1)));
                })
                .onErrorResume(e1->{
                    if(e1 instanceof ActorNotFound)
                        return Mono
                                .just(1)
                                .map(i->sys.actorOf(actor_namespace.props(),fullname))
                                .onErrorResume(e2->{
                                    if(e2 instanceof InvalidActorNameException){
                                        if(e2.getMessage().contains("is not unique!"))
                                            return find_or_build(fullname,sys);
                                    }
                                    return Mono.error(e2);
                                });
                    else
                        return Mono.error(e1);
                })
                ;
    }

    @Builder
    public static class create_child {
        public Props props;
        public String name;
    }

    public static interface msg_join<R>{
        R apply(actor_namespace actor_object);
    }
}
