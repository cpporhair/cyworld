package com.cyworld.social.data.actors.common;

import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.actor.Props;
import akka.actor.ReceiveTimeout;
import com.cyworld.social.data.utils.Coordinator_service_manager;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Duration;

public class actor_entity_child_transaction<DATA_TYPE> extends AbstractActor {
    public static Props props(Object data) {
        return Props.create(actor_entity_child_transaction.class, data);
    }

    public actor_entity_child_transaction(DATA_TYPE data){
        this.data=data;
        getContext().setReceiveTimeout(Duration.ofSeconds(10));
    }

    DATA_TYPE data;
    int check_count=0;

    @Override
    public AbstractActor.Receive createReceive() {
        return receiveBuilder()
                .match(actor_entity.msg_inject_ask.class, msg -> {
                    sender().tell(msg.apply(data), self());
                })
                .match(actor_entity.msg_inject_tell.class, msg -> {
                    msg.apply(data);
                })
                .match(actor_transaction.msg_commit.class, msg -> {
                    getContext().parent().tell
                            (
                                    ((actor_entity.msg_tx_committed) () -> {
                                        return this.data;
                                    }),
                                    self()
                            );
                    getContext().stop(self());
                })
                .match(actor_transaction.msg_undo.class, msg -> {
                    getContext().parent().tell
                            (
                                    ((actor_entity.msg_tx_undone) () -> {
                                        return null;
                                    }),
                                    self()
                            );
                    getContext().stop(self());
                })
                .matchEquals("commit",msg->{
                    getContext().parent().tell
                            (
                                    ((actor_entity.msg_tx_committed) () -> {
                                        return this.data;
                                    }),
                                    self()
                            );
                    getContext().stop(self());
                })
                .matchEquals("undo",msg->{
                    getContext().parent().tell
                            (
                                    ((actor_entity.msg_tx_undone) () -> {
                                        return null;
                                    }),
                                    self()
                            );
                    getContext().stop(self());
                })
                .matchEquals("check_coordinator",msg->{
                    Mono.just(Coordinator_service_manager.cached_instance.getIfPresent("transaction"))
                            .flatMap(coordinator->{
                                return WebClient.create()
                                        .get()
                                        .uri("http://" + coordinator.host + "/transaction/get_transaction_status/" + self().path().name())
                                        .exchange()
                                        .flatMap(clientResponse -> {
                                            switch (clientResponse.statusCode()){
                                                case OK:
                                                    return clientResponse.bodyToMono(Integer.class);
                                                default:
                                                    return Mono.just(-1);
                                            }
                                        })
                                        .onErrorReturn(-1);
                            })
                            .doOnSuccess(status->{
                                switch (status){
                                    case 0:
                                        self().tell("undo", ActorRef.noSender());
                                        return;
                                    case 1:
                                        self().tell("commit",ActorRef.noSender());
                                        return;
                                    default:
                                        check_count++;
                                        if(check_count<11)
                                            context().system()
                                                    .scheduler()
                                                    .scheduleOnce(
                                                            Duration.ofSeconds(1),
                                                            self(),
                                                            "check_coordinator",
                                                            context().system().dispatcher(),
                                                            self()
                                                    );
                                        else
                                            self().tell("undo",ActorRef.noSender());
                                        return;
                                }
                            })
                            .subscribe();
                })
                .match(ReceiveTimeout.class, c -> {
                    self().tell("check_coordinator",self());
                })
                .matchAny(msg->{
                    System.out.println("1111");
                })
                .build();
    }
}
