package com.cyworld.social.data.actors;

import akka.actor.ActorRef;

import static akka.pattern.Patterns.ask;

public class public_message_define {
    public static class signal {
    }

    public final static signal CLOSE_SIGNAL = new signal();
    public final static signal SAVE_SIGNAL = new signal();
    public final static signal SAVE_NOW_SIGNAL = new signal();


    public static interface msg_start_transcation{
        ActorRef apply(String txid);
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
}
