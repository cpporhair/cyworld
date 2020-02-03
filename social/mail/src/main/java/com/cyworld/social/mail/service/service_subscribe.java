package com.cyworld.social.mail.service;

import com.cyworld.social.utils.request.mail.req_new_subscribe;
import com.cyworld.social.mail.utils.data_service_manager;
import lombok.Builder;
import lombok.Data;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

@Component
public class service_subscribe {
    @Data
    @Builder
    private static class context_new_user {
        req_new_subscribe this_req;
        data_service_manager.data_service_instance instance;
    }

    @Autowired
    private data_service_manager dataServiceManager;

    public Mono serv(ServerRequest request) {
        context_new_user context = context_new_user
                .builder()
                .instance(dataServiceManager.cached_instance.getIfPresent("mail"))
                .build();
        return request.bodyToMono(req_new_subscribe.class)
                .flatMap(body -> {
                    context.this_req = body;
                    return WebClient.create()
                            .post()
                            .uri("http://" + context.instance.host + "/mail/v1/subscribe")
                            .contentType(MediaType.APPLICATION_JSON)
                            .body(BodyInserters.fromObject(context.this_req))
                            .exchange();
                })
                .flatMap(res -> {
                    switch (res.statusCode()) {
                        case OK:
                            return ServerResponse
                                    .status(res.statusCode())
                                    .headers(o -> res.headers().asHttpHeaders().forEach(o::addAll))
                                    .body(BodyInserters.fromObject("all done!"));
                        default:
                            return res.bodyToMono(String.class).flatMap(r -> Mono.error(new Throwable(r)));
                    }
                });
    }
}
