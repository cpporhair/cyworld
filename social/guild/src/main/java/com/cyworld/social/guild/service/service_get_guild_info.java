package com.cyworld.social.guild.service;

import com.alibaba.fastjson.JSON;
import com.cyworld.social.guild.utils.data_service_manager;
import com.cyworld.social.guild.utils.game_service_manager;
import com.cyworld.social.utils.request.guild.req_get_guild_info;
import com.cyworld.social.utils.response.guild.res_get_guild_info;
import lombok.Builder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;
@Component
public class service_get_guild_info {
    @Builder
    public static class context {
        req_get_guild_info req;
        res_get_guild_info res;
        data_service_manager.data_service_instance guild_instance;
    }
    @Autowired
    private data_service_manager dataServiceManager;
    @Autowired
    private game_service_manager gameServiceManager;

    public Mono serv(ServerRequest request){
        context this_conext = context
                .builder()
                .guild_instance(dataServiceManager.cached_instance.getIfPresent("guild"))
                .build();
        return request.bodyToMono(req_get_guild_info.class)
                .doOnNext(req->{
                    this_conext.req=req;
                })
                .flatMap(req->{
                    return WebClient.create()
                            .post()
                            .uri("http://" + this_conext.guild_instance.host + "/guild/v1/get_guild_info")
                            .contentType(MediaType.APPLICATION_JSON)
                            .body(BodyInserters.fromObject(this_conext.req))
                            .exchange()
                            .flatMap(res->{
                                switch (res.statusCode()){
                                    case OK:
                                        return res.bodyToMono(res_get_guild_info.class);
                                    default:
                                        return res.bodyToMono(String.class).flatMap(str->Mono.error(new Throwable(str)));
                                }
                            });
                })
                .flatMap(info->{
                    if(info.getEmpty_flag()==1){
                        return Mono.just(info);
                    }
                    else{
                        return Mono.just(info)
                                .flatMap(one_res->{
                                    game_service_manager.game_service_instance game_instance
                                            = gameServiceManager.cached_instance.getIfPresent(one_res.getOwner_namespace());
                                    if (game_instance == null)
                                        return Mono.error(new Throwable("can not find gameserver"));
                                    else
                                        return WebClient.create()
                                                .get()
                                                .uri("http://" + game_instance.host + "/guild/get_owner/" + one_res.getOwner_id())
                                                .exchange()
                                                .flatMap(res->{
                                                    switch (res.statusCode()){
                                                        case OK:
                                                            return res.bodyToMono(String.class)
                                                                    .flatMap(str->{
                                                                        one_res.setOwner_attachment_json(str);
                                                                        return Mono.just(one_res);
                                                                    });
                                                        default:
                                                            return res.bodyToMono(String.class)
                                                                    .flatMap(str->{
                                                                        return Mono.just(one_res);
                                                                    });
                                                    }
                                                });
                                });
                    }

                })
                .flatMap(res->{
                    return ServerResponse.ok().contentType(MediaType.APPLICATION_JSON).body(BodyInserters.fromObject(res));
                });
    }
}
