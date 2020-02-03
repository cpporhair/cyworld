package com.cyworld.social.global.service;

import com.cyworld.social.global.utils.data_service_manager;
import com.cyworld.social.utils.request.global.req_new_server;
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
public class service_new_gameserver {
    private static final Logger logger = LoggerFactory.getLogger(service_new_gameserver.class.getName());
    //private static final Logger logger = util_logger_factory.getLogger();
    @Autowired
    private data_service_manager dataServiceManager;

    @Data
    @Builder
    private static class context {
        req_new_server req;
        String chatroom_id;
        data_service_manager.data_service_instance instance;
    }

    public service_new_gameserver() {
    }

    public Mono serv(ServerRequest req) {
        context this_context = service_new_gameserver.context.builder()
                .instance(dataServiceManager.cached_instance.getIfPresent("mail"))
                .build();
        logger.info(req.toString());
        return req.bodyToMono(req_new_server.class)
                .map(r -> {
                    this_context.req = r;
                    return this_context;
                })
                .flatMap(c -> {
                    return WebClient.create()
                            .post()
                            .uri("http://" + this_context.instance.host + "/mail/v1/mailgroups")
                            .contentType(MediaType.APPLICATION_JSON)
                            .body(BodyInserters.fromObject(this_context.req))
                            .exchange();
                })
                .flatMap(r -> {
                    switch (r.statusCode()) {
                        case OK:
                            return ServerResponse.ok()
                                    .body(BodyInserters.fromObject(this_context.chatroom_id));
                        default:
                            return r.bodyToMono(String.class).flatMap(t -> Mono.error(new Throwable(t)));
                    }
                });
    }
}
