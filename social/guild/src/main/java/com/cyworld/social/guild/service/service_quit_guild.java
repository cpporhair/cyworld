package com.cyworld.social.guild.service;

import com.alibaba.fastjson.JSON;
import com.cyworld.social.guild.utils.data_service_manager;
import com.cyworld.social.utils.request.guild.req_quit_guild;
import com.cyworld.social.utils.request.mail.req_new_subscribe;
import com.cyworld.social.utils.response.common.throwable_res;
import com.cyworld.social.utils.response.guild.res_quit_guild;
import lombok.Builder;
import org.apache.logging.log4j.util.Strings;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Scheduler;
import reactor.core.scheduler.Schedulers;

import java.time.Duration;
import java.util.BitSet;
import java.util.UUID;
import java.util.concurrent.Executors;

@Component
public class service_quit_guild {
    @Builder
    public static class context {
        req_quit_guild req;
        res_quit_guild res;
        String chat_group_id;
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

    public Mono serv(ServerRequest request){
        context this_conext = context
                .builder()
                .guild_instance(dataServiceManager.cached_instance.getIfPresent("guild"))
                .mail_instance(dataServiceManager.cached_instance.getIfPresent("mail"))
                .build();
        return request.bodyToMono(req_quit_guild.class)
                .doOnNext(req->{
                    this_conext.req=req;
                })
                .flatMap(req->{
                    return WebClient.create()
                            .post()
                            .uri("http://" + this_conext.guild_instance.host + "/guild/v1/quit_guild")
                            .contentType(MediaType.APPLICATION_JSON)
                            .body(BodyInserters.fromObject(this_conext.req))
                            .exchange()
                            .flatMap(res->{
                                switch (res.statusCode()){
                                    case OK:
                                        return res.bodyToMono(String.class).map(str->{
                                            res_quit_guild r= JSON.parseObject(str,res_quit_guild.class);
                                            return r;
                                        });
                                    default:
                                        return res.bodyToMono(String.class).flatMap(str->Mono.error(new Throwable(str)));
                                }
                            });
                })
                .flatMap(res->{
                    switch (res.getResult_value()){
                        case res_quit_guild.result_ok:
                            this_conext.res=res;
                            this_conext.guild_quit=true;
                            action_on_quit_guild.do_on_quit
                                    (
                                            this_conext.req.getPlayer_namespace(),
                                            this_conext.req.getPlayer_id(),
                                            this_conext.req.getGuild_id(),
                                            this_conext.mail_instance.getHost()
                                    );
                            return Mono.just(res);
                        default:
                            return Mono.error(new throwable_res(String.valueOf(res.getResult_value())));
                    }
                })
                .map(str->{
                    return this_conext.res;
                })
                .onErrorResume(e->{
                    if(e instanceof throwable_res)
                        return Mono.just(new res_quit_guild(Integer.valueOf(e.getMessage())));
                    else
                        return Mono.error(e);
                })
                .flatMap(res->{
                    return ServerResponse.ok()
                            .contentType(MediaType.APPLICATION_JSON)
                            .body(BodyInserters.fromObject(res));
                });
    }
}
