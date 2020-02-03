package com.cyworld.social.data.service.transaction;

import akka.actor.ActorRef;
import com.cyworld.social.data.utils.static_define;
import org.springframework.web.reactive.function.server.ServerRequest;

import static akka.pattern.Patterns.ask;

import java.time.Duration;
import java.util.concurrent.CompletionStage;

public final class srv_helper {
    public static String get_transaction_token(ServerRequest req) {
        return req.headers().header("tx_token").stream().findFirst().orElse(static_define.no_in_transaction);
    }

    public static <T, R> CompletionStage<R> ask(ActorRef ref, T msg) {
        return akka.pattern.Patterns.ask(ref, msg, Duration.ofSeconds(1)).thenApply(o -> {
            return (R) o;
        });
    }
}
