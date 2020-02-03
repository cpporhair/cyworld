package com.cyworld.social.mail.service;

import com.cyworld.social.utils.request.global.req_new_server;
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

@Component
public class service_new_mail_group {
    private static final Logger logger = LoggerFactory.getLogger(service_new_mail_group.class.getName());
    @Autowired
    private data_service_manager dataServiceManager;

    @Data
    @Builder
    private static class context_new_mail_group {
        req_new_server req;
        data_service_manager.data_service_instance instance;
    }

    public Mono serv(ServerRequest req) {
        context_new_mail_group context = context_new_mail_group.builder()
                .instance(dataServiceManager.cached_instance.getIfPresent("mail"))
                .build();
        return req.bodyToMono(req_new_server.class)
                .map(r -> {
                    context.req = r;
                    return context;
                })
                .flatMap(c -> {
                    return WebClient.create()
                            .post()
                            .uri("http://" + context.instance.host + "/mail/v1/mailgroups")
                            .contentType(MediaType.APPLICATION_JSON)
                            .body(BodyInserters.fromObject(c.req))
                            .exchange();
                })
                .flatMap(r -> {
                    switch (r.statusCode()) {
                        case OK:
                            return ServerResponse.ok().body(BodyInserters.fromObject("OK"));
                        default:
                            return r.bodyToMono(String.class).flatMap(t -> Mono.error(new Throwable(t)));
                    }
                });
    }
}
