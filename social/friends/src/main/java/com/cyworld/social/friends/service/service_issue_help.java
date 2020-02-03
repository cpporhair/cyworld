package com.cyworld.social.friends.service;

import com.cyworld.social.friends.utils.data_service_manager;
import com.cyworld.social.utils.request.friend.req_issue_help;
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
public class service_issue_help {
    @Builder
    public static class context {
        req_issue_help req;
        data_service_manager.data_service_instance instance;
    }

    @Autowired
    private data_service_manager dataServiceManager;

    public Mono serv(ServerRequest request) {
        context this_conext = context
                .builder()
                .instance(dataServiceManager.cached_instance.getIfPresent("friend"))
                .build();
        return request.bodyToMono(req_issue_help.class)
                .doOnNext(body -> {
                    this_conext.req = body;
                })
                .flatMap(body -> {
                    return WebClient.create()
                            .post()
                            .uri("http://" + this_conext.instance.host + "/friend/v1/issue_help")
                            .contentType(MediaType.APPLICATION_JSON)
                            .body(BodyInserters.fromObject(body))
                            .exchange();
                })
                .flatMap(res -> {
                    switch (res.statusCode()) {
                        case OK:
                            return res.bodyToMono(String.class)
                                    .flatMap(s -> {
                                        return ServerResponse.ok().body(BodyInserters.fromObject(s));
                                    });
                        default:
                            return res.bodyToMono(String.class).flatMap(t -> Mono.error(new Throwable(t)));
                    }
                });
    }
}
