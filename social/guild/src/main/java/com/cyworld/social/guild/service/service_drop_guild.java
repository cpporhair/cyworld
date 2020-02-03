package com.cyworld.social.guild.service;

import com.alibaba.fastjson.JSON;
import com.cyworld.social.guild.utils.data_service_manager;
import com.cyworld.social.utils.request.guild.req_drop_guild;
import com.cyworld.social.utils.request.mail.req_drop_mailgroup;
import com.cyworld.social.utils.request.mail.req_new_subscribe;
import com.cyworld.social.utils.response.guild.res_drop_guild;
import lombok.Builder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

import java.time.Duration;
@Component
public class service_drop_guild {
    @Builder
    public static class context {
        req_drop_guild req;
        res_drop_guild res;
        String mail_group_id;
        String tx_token;
        data_service_manager.data_service_instance guild_instance;
        data_service_manager.data_service_instance mail_instance;
        boolean guild_quit=false;
        boolean char_group_quit=false;
        boolean mail_group_quit=false;
    }
    @Autowired
    private data_service_manager dataServiceManager;

    private void do_on_droped(context this_context){
        req_new_subscribe unsubscribe_req = new req_new_subscribe();
        unsubscribe_req.setPlayer(this_context.req.getPlayer_id());
        unsubscribe_req.setServer(this_context.req.getPlayer_namespace());
        unsubscribe_req.setGroups("guild.".concat(this_context.req.getGuild_id()));

        WebClient.create()
                .post()
                .uri("http://" + this_context.mail_instance.host + "/mail/v1/unsubscribe")
                .contentType(MediaType.APPLICATION_JSON)
                .body(BodyInserters.fromObject(unsubscribe_req))
                .exchange()
                //.subscribeOn(Schedulers.fromExecutor(Executors.newSingleThreadExecutor()))
                .flatMap(clientResponse -> {
                    switch (clientResponse.statusCode()){
                        case OK:
                            return Mono.just(1);
                        default:
                            return Mono.error(new Throwable());
                    }
                })
                .retryBackoff(20, Duration.ofSeconds(30))
                .log()
                .subscribe();
        req_drop_mailgroup drop_req=new req_drop_mailgroup();

        drop_req.setMail_group_id("guild.".concat(this_context.req.getGuild_id()));

        WebClient.create()
                .post()
                .uri("http://" + this_context.mail_instance.host + "/mail/v1/drop_mailgroups")
                .contentType(MediaType.APPLICATION_JSON)
                .body(BodyInserters.fromObject(drop_req))
                .exchange()
                //.subscribeOn(Schedulers.fromExecutor(Executors.newSingleThreadExecutor()))
                .flatMap(clientResponse -> {
                    switch (clientResponse.statusCode()){
                        case OK:
                            return Mono.just(1);
                        default:
                            return Mono.error(new Throwable());
                    }
                })
                .retryBackoff(20, Duration.ofSeconds(30))
                .log()
                .subscribe();

    }

    public Mono serv(ServerRequest request){
        context this_conext = context
                .builder()
                .guild_instance(dataServiceManager.cached_instance.getIfPresent("guild"))
                .mail_instance(dataServiceManager.cached_instance.getIfPresent("mail"))
                .build();
        return request.bodyToMono(req_drop_guild.class)
                .doOnNext(req->{
                    this_conext.req=req;
                })
                .flatMap(req->{
                    return WebClient.create()
                            .post()
                            .uri("http://" + this_conext.guild_instance.host + "/guild/v1/drop_guild")
                            .contentType(MediaType.APPLICATION_JSON)
                            .body(BodyInserters.fromObject(this_conext.req))
                            .exchange()
                            .flatMap(res->{
                                switch (res.statusCode()){
                                    case OK:
                                        return res.bodyToMono(String.class).map(str->{
                                            res_drop_guild r= JSON.parseObject(str,res_drop_guild.class);
                                            return r;
                                        });
                                    default:
                                        return res.bodyToMono(String.class).flatMap(str->Mono.error(new Throwable(str)));
                                }
                            });
                })
                .flatMap(res->{
                    switch (res.getResult_value()){
                        case res_drop_guild.result_ok:
                            do_on_droped(this_conext);
                            return ServerResponse.ok()
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .body(BodyInserters.fromObject(res));
                        default:
                            return ServerResponse.ok()
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .body(BodyInserters.fromObject(res));
                    }
                });
    }
}
