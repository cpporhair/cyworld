package com.cyworld.social.mail.service;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.cyworld.social.utils.request.mail.req_new_broadcast_mail;
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
//s
@Component
public class service_new_broadcast_mail {
    private static final Logger logger = LoggerFactory.getLogger(service_new_broadcast_mail.class.getName());

    @Autowired
    private data_service_manager dataServiceManager;

    @Data
    @Builder
    public static class context {
        String s;
        req_new_broadcast_mail req;
        data_service_manager.data_service_instance instance;
    }

    public Mono serv(ServerRequest request) {
        context this_conext = context
                .builder()
                .instance(dataServiceManager.cached_instance.getIfPresent("mail"))
                .build();
        return request.bodyToMono(req_new_broadcast_mail.class)
                .doOnNext(req -> {
                    this_conext.req = req;
                })
                .flatMap(c -> {
                    if (this_conext.req.getAttechmentJson() == null)
                        return Mono.just(c);
                    for (String s : this_conext.req.getAttechmentJson()) {
                        JSONObject o = JSON.parseObject(s);
                        if (!o.containsKey("id"))
                            return Mono.error(new Throwable("error attachment"));
                        if (!o.containsKey("content"))
                            return Mono.error(new Throwable("error attachment"));
                    }
                    return Mono.just(c);
                })
                .flatMap(req -> {
                    return WebClient.create()
                            .post()
                            .uri("http://" + this_conext.instance.host + "/mail/v1/broadcast")
                            .contentType(MediaType.APPLICATION_JSON_UTF8)
                            .body(BodyInserters.fromObject(this_conext.req))
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
