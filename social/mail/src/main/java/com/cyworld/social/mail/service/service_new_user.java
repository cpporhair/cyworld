package com.cyworld.social.mail.service;

import com.cyworld.social.utils.request.mail.req_new_subscribe;
import com.cyworld.social.utils.request.global.req_new_user;
import com.cyworld.social.mail.utils.transaction_helper;
import com.cyworld.social.mail.utils.data_service_manager;
import lombok.Builder;
import lombok.Data;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

import java.util.UUID;

@Component
public class service_new_user {
    private static final Logger logger = LoggerFactory.getLogger(service_new_user.class.getName());
    @Autowired
    private data_service_manager dataServiceManager;

    @Data
    @Builder
    private static class context_new_user {
        req_new_user this_req;
        data_service_manager.data_service_instance instance;
        String tx_token;
    }


    public Mono<ServerResponse> serv(ServerRequest req) {
        context_new_user context = context_new_user.builder()
                .instance(dataServiceManager.cached_instance.getIfPresent("mail"))
                .build();

        return req.bodyToMono(req_new_user.class)
                .map(body -> {
                    context.setThis_req(body);
                    context.setTx_token(UUID.randomUUID().toString());
                    return body;
                })
                .flatMap(body -> {
                    return WebClient.create()
                            .post()
                            .uri("http://" + context.instance.host + "/mail/v1/users")
                            .header("tx_token", context.tx_token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .body(BodyInserters.fromObject(context.this_req))
                            .exchange();
                })
                .flatMap(res -> {
                    switch (res.statusCode()) {
                        case OK:
                            req_new_subscribe next_req = new req_new_subscribe();
                            next_req.setPlayer(context.this_req.getPlayer_id());
                            next_req.setServer(context.this_req.getNamespace());
                            next_req.setGroups(context.this_req.getNamespace());
                            return WebClient.create()
                                    .post()
                                    .uri("http://" + context.instance.host + "/mail/v1/subscribe")
                                    .header("tx_token", context.tx_token)
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .body(BodyInserters.fromObject(next_req))
                                    .exchange();
                        default:
                            return res.bodyToMono(String.class).flatMap(r -> Mono.error(new Throwable(r)));
                    }
                })
                .flatMap(res -> {
                    switch (res.statusCode()) {
                        case OK:
                            return transaction_helper
                                    .commit_Mono(context.instance.host, context.tx_token)
                                    .flatMap(commit_res -> {
                                        switch (commit_res.statusCode()) {
                                            case OK:
                                                return ServerResponse
                                                        .status(res.statusCode())
                                                        .headers(o -> res.headers().asHttpHeaders().forEach(o::addAll))
                                                        .body(BodyInserters.fromObject("all done!"));
                                            default:
                                                return res.bodyToMono(String.class)
                                                        .flatMap(r -> Mono.error(new Throwable(r)));
                                        }
                                    });

                        default:
                            return res.bodyToMono(String.class).flatMap(r -> Mono.error(new Throwable(r)));
                    }
                })
                .doOnError(e -> {
                    transaction_helper
                            .undo_Mono(context.instance.host, context.tx_token)
                            .subscribe();
                }).log();
    }
}
