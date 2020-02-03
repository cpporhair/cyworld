package com.cyworld.social.data.service.test;

import akka.actor.ActorRef;
import akka.actor.PoisonPill;
import com.cyworld.social.data.actors.akka_system;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

import java.util.concurrent.CompletableFuture;

@Component
public class srv_clear_all_forTest {
    @Autowired
    akka_system akka;

    public Mono serv(ServerRequest request) {
        akka.actor_system.actorSelection("/user/*").tell(PoisonPill.getInstance(), ActorRef.noSender());
        return ServerResponse.ok().body(BodyInserters.fromObject("OK"));
    }
}
