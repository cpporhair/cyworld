package com.cyworld.social.data.entity.base;

import reactor.core.publisher.Mono;

public interface i_mongoable<T> {
    Mono<T> save_mono();
    Mono<T> load_mono();
}
