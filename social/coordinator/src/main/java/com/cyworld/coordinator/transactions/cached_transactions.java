package com.cyworld.coordinator.transactions;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

@Configuration
public class cached_transactions {
    public Cache<String,transaction> cache = Caffeine.newBuilder()
            .maximumSize(100000)
            .expireAfterAccess(Duration.ofSeconds(60*5))
            .build();
}
