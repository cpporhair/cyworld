package com.cyworld.social.data.actors.common;

import akka.actor.AbstractActorWithStash;
import akka.actor.ActorRef;
import akka.actor.ReceiveTimeout;
import com.cyworld.social.data.actors.public_message_define;
import com.cyworld.social.data.entity.base.i_deep_clone;
import com.cyworld.social.data.utils.static_define;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Mono;

import java.time.Duration;

public abstract class actor_entity<DATA_TYPE extends i_deep_clone<DATA_TYPE>> extends AbstractActorWithStash {

    public Receive lock;
    public Receive work;

    protected static final Logger logger = LoggerFactory.getLogger(actor_entity.class.getName());
    protected DATA_TYPE data;
    protected String thisToken;
    boolean is_new=true;
    boolean saving=false;

    protected boolean auto_close(){
        return true;
    }

    protected actor_entity(DATA_TYPE data,boolean is_new){
        this.data=data;
        this.is_new=is_new;
        logger.info(self().path().toString().concat(" start actor"));
        if(auto_close())
            getContext().setReceiveTimeout(Duration.ofSeconds(static_define.data_actor_expire_second));
    }

    protected abstract Mono<DATA_TYPE> save_mono(DATA_TYPE data);

    private void init_receive_builder(){
        lock=receiveBuilder()
                .match(msg_get_transaction.class, msg -> {
                    if (thisToken == null)
                        return;
                    if (!msg.token().equals(thisToken)){
                        stash();
                        logger.info("stash ".concat(self().path().toString()).concat(" ").concat(msg.token()));
                        return;
                    }
                    sender().tell(getContext().getChild(thisToken), self());
                })
                .match(msg_readonly_inject_tell.class, msg -> {
                    msg.apply(data);
                })
                .match(msg_readonly_inject_ask.class, msg -> {
                    sender().tell(msg.apply(data), ActorRef.noSender());
                })
                .match(msg_tx_undone.class,msg->{
                    thisToken = null;
                    if(is_new){
                        getContext().stop(self());
                        logger.info("actor ".concat(self().path().toString()).concat(" stoped."));
                    }
                    else{
                        unstashAll();
                    }
                    getContext().unbecome();
                })
                .match(msg_tx_committed.class, msg -> {
                    this.data = ((DATA_TYPE) msg.data());
                    thisToken = null;
                    unstashAll();
                    getContext().unbecome();
                    self().tell(public_message_define.SAVE_SIGNAL,self());
                })
                .matchAny(m -> {
                    stash();
                })
                .build();
        work = receiveBuilder()
                .match(msg_readonly_inject_tell.class, msg -> {
                    msg.apply(data);
                })
                .match(msg_readonly_inject_ask.class, msg -> {
                    sender().tell(msg.apply(data), ActorRef.noSender());
                })
                .match(msg_inject_ask.class, msg -> {
                    sender().tell(msg.apply(data), self());
                })
                .match(msg_inject_tell.class, msg -> {
                    msg.apply(data);
                })
                .match(msg_get_transaction.class, msg -> {
                    if (thisToken == null) {
                        thisToken = msg.token();
                        logger.info("lock ".concat(self().path().toString()).concat(" ").concat(thisToken));
                        getContext().become(lock);
                        sender().tell
                                (
                                        getContext().actorOf
                                                (
                                                        actor_entity_child_transaction.props(this.data.deep_clone()),
                                                        thisToken
                                                ),
                                        self()
                                );
                    }
                })
                .matchEquals(public_message_define.CLOSE_SIGNAL, msg -> {
                    getContext().stop(self());
                })
                .match(ReceiveTimeout.class, t -> {
                    getContext().stop(self());
                })
                .matchEquals(public_message_define.SAVE_NOW_SIGNAL,msg->{
                    save_mono(data.deep_clone()).subscribe();
                })
                .matchEquals(public_message_define.SAVE_SIGNAL,msg->{
                    is_new=false;
                    if(saving)
                        return;
                    saving=true;
                    getContext().getSystem()
                            .scheduler()
                            .scheduleOnce(
                                    Duration.ofSeconds(1),
                                    self(),
                                    "inner_save",
                                    context().system().dispatcher(),
                                    self());
                })
                .matchEquals("inner_save",msg->{
                    saving=false;
                    save_mono(data.deep_clone()).subscribe();
                })
                .match(String.class, i -> {
                    logger.info(i);
                })
                .build();
    }

    public static interface msg_get_transaction {
        String token();
    }

    public static interface msg_tx_committed {
        Object data();
    }
    public static interface msg_tx_undone {
        Object data();
    }

    public static interface msg_inject_tell<T>{
        void apply(T data);
    }
    public static interface msg_inject_ask<T,R>{
        R apply(T data);
    }
    public static interface msg_readonly_inject_tell<T>{
        void apply(T clone);
    }
    public static interface msg_readonly_inject_ask<T,R>{
        R apply(T clone);
    }

    @Override
    public Receive createReceive() {
        init_receive_builder();
        return work;
    }

    public static Mono<ActorRef> join_transaction(ActorRef ref,String token){
        return Mono.fromCompletionStage
                (
                        akka.pattern.Patterns.ask
                                (
                                        ref,
                                        (msg_get_transaction) (() -> {
                                            return token;
                                        }),
                                        Duration.ofSeconds(1)
                                )
                                .thenApply(ActorRef.class::cast)
                );
    }
}
