package com.cyworld.social.ranks.service;

import com.cyworld.social.ranks.utils.data_service_manager;
import com.cyworld.social.utils.request.ranks.req_get_ranks;
import lombok.Builder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

@Component
public class service_get_ranks {
    @Builder
    public static class context {
        req_get_ranks req;
        data_service_manager.data_service_instance instance;
    }

    @Autowired
    private data_service_manager dataServiceManager;

    public Mono serv(ServerRequest request) {
        context this_conext = context
                .builder()
                .instance(dataServiceManager.cached_instance.getIfPresent("ranks"))
                .build();
        return request.bodyToMono(req_get_ranks.class)
                .doOnNext(body -> {
                    this_conext.req = body;
                })
                .flatMap(body -> {
                    return WebClient.create()
                            .post()
                            .uri("http://" + this_conext.instance.host + "/rank/v1/get_ranks")
                            .contentType(MediaType.APPLICATION_JSON)
                            .body(BodyInserters.fromObject(body))
                            .exchange();
                })
                .flatMap(res -> {
                    switch (res.statusCode()) {
                        case OK:
                            return res.bodyToMono(String.class)
                                    .flatMap(s -> {
                                        return ServerResponse.ok().contentType(MediaType.APPLICATION_JSON).body(BodyInserters.fromObject(s));
                                    });
                        default:
                            return res.bodyToMono(String.class).flatMap(t -> Mono.error(new Throwable(t)));
                    }
                });
    }
}
