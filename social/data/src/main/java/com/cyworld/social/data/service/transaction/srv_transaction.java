package com.cyworld.social.data.service.transaction;

import akka.actor.ActorRef;
import com.cyworld.social.data.actors.common.actor_transaction;
import com.cyworld.social.data.actors.akka_system;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

import java.time.Duration;

import static akka.pattern.Patterns.ask;

@Component
public class srv_transaction {
    private static final Logger logger = LoggerFactory.getLogger(srv_transaction.class.getName());
    @Autowired
    cached_transaction cached_tx;
    @Autowired
    akka_system akka;

    public Mono commit(ServerRequest req) {
        return req.bodyToMono(String.class)
                .flatMap(s -> {
                    logger.info("transaction commit token=".concat(s));
                    ActorRef r = akka.actor_system.actorFor(actor_transaction.compute_search_path(s));
                    if (r.isTerminated())
                        return Mono.error(new Throwable("invalid token or terminated"));
                    else
                        return Mono
                                .fromCompletionStage(ask(r, "commit", Duration.ofSeconds(5)).thenApply(String.class::cast))
                                .flatMap(res -> {
                                    switch (res) {
                                        case "successed":
                                            return ServerResponse
                                                    .ok()
                                                    .body(BodyInserters.fromObject("OK"));
                                        case "fault":
                                        default:
                                            return Mono.error(new Throwable("commit fault"));
                                    }
                                });
                });
    }

    public Mono undo(ServerRequest req) {
        return req.bodyToMono(String.class)
                .flatMap(s -> {
                    logger.info("transaction undo token=".concat(s));
                    ActorRef r = akka.actor_system.actorFor(actor_transaction.compute_search_path(s));
                    if (r.isTerminated())
                        return Mono.error(new Throwable("invalid token or terminated"));
                    else
                        return Mono
                                .fromCompletionStage(
                                        ask(r, "undo", Duration.ofSeconds(5))
                                                .thenApply(String.class::cast)
                                                .exceptionally(t -> {
                                                    return "timeout";
                                                })
                                )
                                .flatMap(res -> {
                                    switch (res) {
                                        case "successed":
                                            return ServerResponse
                                                    .ok()
                                                    .body(BodyInserters.fromObject("OK"));
                                        case "fault":
                                        default:
                                            return Mono.error(new Throwable(res));
                                    }
                                });
                });
    }
}
