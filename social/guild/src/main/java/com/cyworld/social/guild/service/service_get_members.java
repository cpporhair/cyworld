package com.cyworld.social.guild.service;

import com.alibaba.fastjson.JSON;
import com.cyworld.social.guild.utils.data_service_manager;
import com.cyworld.social.guild.utils.game_service_manager;
import com.cyworld.social.utils.request.guild.req_get_members;
import com.cyworld.social.utils.response.guild.res_get_members;
import com.cyworld.social.utils.response.guild.res_get_members_component;
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
import java.util.Map;
import java.util.stream.Collectors;
@Component
public class service_get_members {
    @Builder
    public static class context {
        req_get_members req;
        res_get_members res;
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
        return request.bodyToMono(req_get_members.class)
                .doOnNext(req->{
                    this_conext.req=req;
                })
                .flatMap(req->{
                    return WebClient.create()
                            .post()
                            .uri("http://" + this_conext.guild_instance.host + "/guild/v1/get_members")
                            .contentType(MediaType.APPLICATION_JSON)
                            .body(BodyInserters.fromObject(this_conext.req))
                            .exchange()
                            .flatMap(res->{
                                switch (res.statusCode()){
                                    case OK:
                                        return res.bodyToMono(res_get_members.class);
                                    default:
                                        return res.bodyToMono(String.class).flatMap(str->Mono.error(new Throwable(str)));
                                }
                            });
                })
                .flatMap(res->{
                    Map<String,List<res_get_members_component>> m=res.getComponent().stream().collect(Collectors.groupingBy(e->{
                        return e.getNamespace();
                    }));
                    return Flux.fromIterable(m.entrySet())
                            .flatMap(one_res->{
                                game_service_manager.game_service_instance game_instance
                                        = gameServiceManager.cached_instance.getIfPresent(one_res.getKey());
                                if (game_instance == null)
                                    return Mono.error(new Throwable("can not find gameserver"));
                                else
                                    return WebClient.create()
                                            .post()
                                            .uri("http://" + game_instance.host + "/guild/get_all_members_data")
                                            .contentType(MediaType.APPLICATION_JSON)
                                            .body(BodyInserters.fromObject(one_res.getValue()))
                                            .exchange()
                                            .flatMap(clientResponse->{
                                                switch (clientResponse.statusCode()){
                                                    case OK:
                                                        return clientResponse.bodyToMono(List.class)
                                                                .flatMap(list->{
                                                                    one_res.setValue(list);
                                                                    return Mono.just(one_res);
                                                                });
                                                    default:
                                                        return clientResponse.bodyToMono(String.class)
                                                                .flatMap(str->{
                                                                    return Mono.just(one_res);
                                                                });
                                                }
                                            });
                            })
                            .reduce(new res_get_members(),(r,e)->{
                                r.getComponent().addAll(e.getValue());
                                return r;
                            });

                })
                .flatMap(res->{
                    return ServerResponse.ok().contentType(MediaType.APPLICATION_JSON).body(BodyInserters.fromObject(res));
                });
    }
}
