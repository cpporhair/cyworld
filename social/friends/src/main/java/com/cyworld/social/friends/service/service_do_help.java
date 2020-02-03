package com.cyworld.social.friends.service;

import com.cyworld.social.friends.utils.data_service_manager;
import com.cyworld.social.friends.utils.game_service_manager;
import com.cyworld.social.utils.request.friend.req_do_help;
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
public class service_do_help {
    @Builder
    public static class context {
        req_do_help req;
        data_service_manager.data_service_instance instance;
    }

    @Autowired
    private data_service_manager dataServiceManager;
    @Autowired
    private game_service_manager gameServiceManager;

    public Mono serv(ServerRequest request) {
        context this_conext = context
                .builder()
                .instance(dataServiceManager.cached_instance.getIfPresent("friend"))
                .build();
        return request.bodyToMono(req_do_help.class)
                .doOnNext(body -> {
                    this_conext.req = body;
                })
                .flatMap(body -> {
                    return WebClient.create()
                            .post()
                            .uri("http://" + this_conext.instance.host + "/friend/v1/do_help")
                            .contentType(MediaType.APPLICATION_JSON)
                            .body(BodyInserters.fromObject(body))
                            .exchange();
                })
                .flatMap(res -> {
                    switch (res.statusCode()) {
                        case OK:
                            game_service_manager.game_service_instance instance
                                    = gameServiceManager.cached_instance.getIfPresent(this_conext.req.getTarget_namespace());
                            return WebClient.create()
                                    .get()
                                    .uri("http://" + instance.host + "/friend/dohelp/"
                                            + this_conext.req.getTarget_id()+"/"
                                            +this_conext.req.getNeed_ID()+"/"
                                            +this_conext.req.getAmount())
                                    .exchange();
                        default:
                            return res.bodyToMono(String.class).flatMap(t -> Mono.error(new Throwable(t)));
                    }
                })
                .flatMap(res -> {
                    switch (res.statusCode()) {
                        case OK:
                            return ServerResponse.ok().body(BodyInserters.fromObject("OK"));
                        default:
                            return ServerResponse.badRequest().body(BodyInserters.fromObject("ERR"));
                    }
                });
    }
}
