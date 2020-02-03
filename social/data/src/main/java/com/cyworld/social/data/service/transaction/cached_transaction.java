package com.cyworld.social.data.service.transaction;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import lombok.Builder;
import lombok.Data;
import lombok.NonNull;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import reactor.util.annotation.Nullable;

import java.time.Duration;

@Component
public class cached_transaction {
    @Data
    @Builder
    public static class tx_context {
        String token;
        Mono<?> on_successed;
        Mono<?> on_undo;
    }

    private Cache<String, tx_context> cached_tx_context = Caffeine.newBuilder().maximumSize(100).expireAfterAccess(Duration.ofSeconds(10)).build();

    public void put(tx_context context) {
        tx_context c = cached_tx_context.getIfPresent(context.token);
        if (c == null) {
            cached_tx_context.put(context.token, context);
        } else {
            c.on_successed = context.on_undo.then(c.on_successed);
            c.on_undo = context.on_undo.then(c.on_undo);
        }
    }

    public tx_context pop(String token) {
        tx_context c = cached_tx_context.getIfPresent(token);
        cached_tx_context.invalidate(token);
        return c;
    }
}
