package com.cyworld.social.data.actors;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;

@Component
public class akka_system {
    public final ActorSystem actor_system;

    public akka_system() {
        actor_system = ActorSystem.create("datas");
    }

    @PostConstruct
    private void init() {
    }
}
