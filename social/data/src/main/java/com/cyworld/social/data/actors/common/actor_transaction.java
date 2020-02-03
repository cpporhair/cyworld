package com.cyworld.social.data.actors.common;

import akka.actor.*;
import com.cyworld.social.data.utils.Coordinator_service_manager;
import lombok.Builder;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

public class actor_transaction extends AbstractActor {
    public static interface msg_commit {
        String token();
    }

    public static interface msg_undo {
        String token();
    }

    public static class tx_context {
        private boolean actor_commit_flag = false;
        private boolean actor_undo_flag = false;;

        public Mono<?> mono_commit = null;
        public Mono<?> mono_undo = null;

        public boolean has_commit_flag(){
            return actor_commit_flag;
        }
        public boolean check_and_hold_commit_flag(){
            if(has_commit_flag())
                return true;
            actor_commit_flag=true;
            return false;
        }
        public boolean has_undo_flag(){
            return actor_undo_flag;
        }
        public boolean check_and_hold_undo_flag(){
            if(has_undo_flag())
                return true;
            actor_undo_flag=true;
            return false;
        }
    }

    @Builder
    public static class msg_push_tx_context {
        public String actor_path;
        public Function<tx_context, tx_context> record_commit;
        public Function<tx_context, tx_context> record_undo;
    }

    public static Props props(String token) {
        return Props.create(actor_transaction.class,token);
    }

    Map<String, tx_context> context_map = new HashMap<>();

    public actor_transaction(String token) {
        this.token=token;
        getContext().setReceiveTimeout(Duration.ofSeconds(10));
    }

    Receive waiting;
    Receive commit;
    Receive undo;
    String token;

    @Override
    public Receive createReceive() {
        undo = receiveBuilder()
                .matchEquals("commit", s -> {
                    sender().tell("fault", ActorRef.noSender());
                })
                .matchEquals("undo", s -> {
                    sender().tell("successed", ActorRef.noSender());
                })
                .matchEquals("successed", s -> {
                    sender().tell("successed", ActorRef.noSender());
                })
                .matchEquals("fault", s -> {
                    sender().tell("fault", ActorRef.noSender());
                })
                .match(ReceiveTimeout.class, c -> {
                    context().stop(self());
                })
                .build();
        commit = receiveBuilder()
                .matchEquals("commit", s -> {
                    sender().tell("successed", ActorRef.noSender());
                })
                .matchEquals("undo", s -> {
                    sender().tell("fault", ActorRef.noSender());
                })
                .matchEquals("successed", s -> {
                    sender().tell("successed", ActorRef.noSender());
                })
                .matchEquals("fault", s -> {
                    sender().tell("fault", ActorRef.noSender());
                })
                .match(ReceiveTimeout.class, c -> {
                    context().stop(self());
                })
                .build();
        waiting = receiveBuilder()
                .matchEquals("commit", s -> {
                    getContext().become(commit);
                    Flux.fromIterable(context_map.entrySet())
                            .flatMap(e -> e.getValue().mono_commit)
                            .count()
                            .doFinally(signalType -> {
                                switch (signalType){
                                    case ON_ERROR:
                                    case CANCEL:
                                        self().tell("fault", sender());
                                    case ON_COMPLETE:
                                        self().tell("successed", sender());
                                        return;
                                }
                            })
                            .subscribe();
                })
                .matchEquals("undo", s -> {
                    getContext().become(undo);
                    Flux.fromIterable(context_map.entrySet())
                            .flatMap(e -> e.getValue().mono_undo)
                            .count()
                            .doFinally(signalType -> {
                                switch (signalType){
                                    case ON_ERROR:
                                    case CANCEL:
                                        self().tell("fault", sender());
                                    case ON_COMPLETE:
                                        self().tell("successed", sender());
                                        return;
                                }
                            })
                            .subscribe();
                })
                .match(msg_push_tx_context.class, msg -> {
                    tx_context context = context_map.getOrDefault(msg.actor_path, new tx_context());
                    context = msg.record_commit.apply(context);
                    context = msg.record_undo.apply(context);
                    context_map.put(msg.actor_path, context);
                })
                .matchEquals("check_coordinator",s->{
                    Mono.just(Coordinator_service_manager.cached_instance.getIfPresent("transaction"))
                            .flatMap(coordinator->{
                                return WebClient.create()
                                        .get()
                                        .uri("http://" + coordinator.host + "/transaction/get_transaction_status/" + token)
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
                                        self().tell("undo",ActorRef.noSender());
                                        return;
                                    case 1:
                                        self().tell("commit",ActorRef.noSender());
                                        return;
                                    default:
                                        context().system()
                                                .scheduler()
                                                .scheduleOnce(
                                                        Duration.ofSeconds(1),
                                                        self(),
                                                        "check_coordinator",
                                                        context().system().dispatcher(),
                                                        self()
                                                );
                                        return;
                                }
                            })
                            .subscribe();
                })
                .match(ReceiveTimeout.class, c -> {
                    //self().tell("check_coordinator",self());
                    getContext().stop(self());
                })
                .build();

        return waiting;
    }

    public static String compute_search_path(String token) {
        return "/user/transation.".concat(token);
    }

    public static String compute_create_name(String token) {
        return "transation.".concat(token);
    }

    public static ActorRef find_or_build(String token, ActorSystem system) {
        ActorRef res = system.actorFor(compute_search_path(token));
        if (res.isTerminated())
            res = system.actorOf(actor_transaction.props(token), compute_create_name(token));
        return res;
    }

    public static void simple_msg_push_tx_context_teller(String actor_path,
                                                         String token,
                                                         ActorSystem system){
        simple_msg_push_tx_context_teller(actor_path,token,system,null,null);
    }

    public static void simple_msg_push_tx_context_teller(String actor_path,
                                                         String token,
                                                         ActorSystem system,
                                                         Runnable then_commit,
                                                         Runnable then_undo){
        actor_transaction.find_or_build(token, system)
                .tell(
                        actor_transaction.msg_push_tx_context
                                .builder()
                                .actor_path(actor_path)
                                .record_commit(old -> {
                                    if(old.check_and_hold_commit_flag())
                                        return old;
                                    Mono mono = Mono.just(actor_path)
                                            .doOnNext(path -> {
                                                ActorRef ref=system.actorFor(path);
                                                ref.tell
                                                        (
                                                                ((actor_transaction.msg_commit) () -> {
                                                                    return token;
                                                                }),
                                                                ActorRef.noSender()
                                                        );
                                                if(then_commit!=null)
                                                    then_commit.run();
                                            });
                                    old.mono_commit = old.mono_commit == null
                                            ? mono
                                            : old.mono_commit.then(mono);
                                    return old;
                                })
                                .record_undo(old -> {
                                    if(old.check_and_hold_undo_flag())
                                        return old;
                                    old.mono_undo = Mono
                                            .just(actor_path)
                                            .doOnNext(path -> {
                                                ActorRef ref=system.actorFor(path);
                                                ref.tell
                                                        (
                                                                ((actor_transaction.msg_undo) () -> {
                                                                    return token;
                                                                }),
                                                                ActorRef.noSender()
                                                        );
                                                if(then_undo!=null)
                                                    then_undo.run();
                                            });
                                    return old;
                                })
                                .build(),
                        ActorRef.noSender()
                );
    }
}
