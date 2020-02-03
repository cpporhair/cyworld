package com.cyworld.social.global.service;

import com.alibaba.fastjson.JSONObject;
import com.cyworld.social.global.utils.Coordinator_service_manager;
import com.cyworld.social.global.utils.data_service_manager;
import com.cyworld.social.utils.request.mail.req_new_subscribe;
import com.cyworld.social.utils.request.global.req_new_user;
import com.cyworld.social.utils.transaction.transaction_helper;
import lombok.Builder;
import lombok.Data;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;
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
        data_service_manager.data_service_instance mail_instance;
        data_service_manager.data_service_instance friend_instance;
        String tx_token;
        boolean transaction_started=false;
    }

    public Mono<ServerResponse> serv(ServerRequest req) {
        context_new_user context = context_new_user.builder()
                .mail_instance(dataServiceManager.cached_instance.getIfPresent("mail"))
                .friend_instance(dataServiceManager.cached_instance.getIfPresent("friend"))
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
                            .uri("http://" + context.friend_instance.host + "/friend/v1/new_player_friends_data")
                            .header("tx_token", context.tx_token)
                            .contentType(MediaType.APPLICATION_JSON)
                            //.body(BodyInserters.fromObject("opop"))
                            .body(BodyInserters.fromObject(context.this_req))
                            .exchange();
                })
                .doOnNext(res->{
                    context.transaction_started=true;
                })
                .flatMap(res -> {
                    switch (res.statusCode()){
                        case OK:
                            return WebClient.create()
                                    .post()
                                    .uri("http://" + context.mail_instance.host + "/mail/v1/users")
                                    .header("tx_token", context.tx_token)
                                    .contentType(MediaType.APPLICATION_JSON)
                                    //.body(BodyInserters.fromObject("opop"))
                                    .body(BodyInserters.fromObject(context.this_req))
                                    .exchange();
                        default:
                            return res.bodyToMono(String.class).flatMap(r -> Mono.error(new Throwable(r)));
                    }

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
                                    .uri("http://" + context.mail_instance.host + "/mail/v1/subscribe")
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
                                    .commit_all(
                                            Coordinator_service_manager.cached_instance.getIfPresent("transaction").host,
                                            context.tx_token,
                                            context.mail_instance.host,
                                            context.friend_instance.host
                                    );
                        default:
                            return res.bodyToMono(String.class).flatMap(r -> Mono.error(new Throwable(r)));
                    }
                })
                .flatMap(b->{
                    if(b){
                        return ServerResponse
                                .ok()
                                .body(BodyInserters.fromObject("all done!"));

                    }
                    else{
                        return Mono.error(new Throwable("commit error and undoed"));
                    }
                })
                .doFinally(o->{
                    switch (o){
                        case CANCEL:
                        case ON_ERROR:
                            if(context.transaction_started)
                                transaction_helper
                                        .undo_all(
                                                Coordinator_service_manager.cached_instance.getIfPresent("transaction").host,
                                                context.tx_token,
                                                context.mail_instance.host,
                                                context.friend_instance.host
                                        )
                                        .subscribe();
                            return;
                        case ON_COMPLETE:
                            logger.error(context.tx_token.concat("successed"));
                    }
                })
                .log();
    }
}
