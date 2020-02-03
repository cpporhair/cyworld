package com.cyworld.social.friends.utils;

import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

public class transaction_helper {
    public static Mono<ClientResponse> commit_Mono(String host, String token) {
        return WebClient.create()
                .post()
                .uri("http://" + host + "/inner_data/v1/tx_commit")
                .body(BodyInserters.fromObject(token))
                .exchange();
    }

    public static Mono<ClientResponse> undo_Mono(String host, String token) {
        return WebClient.create()
                .post()
                .uri("http://" + host + "/inner_data/v1/tx_undo")
                .body(BodyInserters.fromObject(token))
                .exchange();
    }
}
