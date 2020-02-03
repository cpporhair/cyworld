package com.cyworld.social.mail.service;

import com.cyworld.social.mail.utils.data_service_manager;
import com.cyworld.social.utils.request.mail.req_refresh_and_get_mail_list;
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
public class service_get_mail_list {

    @Data
    @Builder
    public static class context {
        String s;
        req_refresh_and_get_mail_list req;
        data_service_manager.data_service_instance instance;
        String refresh_mailbox_inbox_response;
    }


    @Autowired
    private data_service_manager dataServiceManager;

    public Mono serv(ServerRequest request) {
        context this_conext = context
                .builder()
                .instance(dataServiceManager.cached_instance.getIfPresent("mail"))
                .build();
        return request.bodyToMono(req_refresh_and_get_mail_list.class)
                .doOnNext(body -> {
                    this_conext.req = body;
                })
                .flatMap(body -> {
                    return WebClient.create()
                            .post()
                            .uri("http://" + this_conext.instance.host + "/mail/v1/refresh")
                            .contentType(MediaType.APPLICATION_JSON)
                            .body(BodyInserters.fromObject(this_conext.req.get_req_refresh_mailbox_inbox()))
                            .exchange();
                })
                .flatMap(res -> {
                    switch (res.statusCode()) {
                        case OK:
                            return res.bodyToMono(String.class)
                                    .doOnNext(s -> this_conext.refresh_mailbox_inbox_response = s)
                                    .flatMap(s -> {
                                        return WebClient.create()
                                                .post()
                                                .uri("http://" + this_conext.instance.host + "/mail/v1/getmaillist")
                                                .contentType(MediaType.APPLICATION_JSON)
                                                .body(BodyInserters.fromObject(this_conext.req.get_req_get_mail_list()))
                                                .exchange();
                                    });
                        default:
                            return res.bodyToMono(String.class).flatMap(t -> Mono.error(new Throwable(t)));
                    }
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
