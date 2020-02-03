package com.cyworld.social.guild.service;

import com.cyworld.social.guild.utils.data_service_manager;
import com.cyworld.social.guild.utils.game_service_manager;
import com.cyworld.social.utils.request.guild.req_get_help_list;
import com.cyworld.social.utils.response.guild.res_get_guild_help_list;
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
public class service_get_guild_help_list {
    @Builder
    public static class context {
        req_get_help_list req;
        res_get_guild_help_list res;
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
        return request.bodyToMono(req_get_help_list.class)
                .doOnNext(req->{
                    this_conext.req=req;
                })
                .flatMap(req->{
                    return WebClient.create()
                            .post()
                            .uri("http://" + this_conext.guild_instance.host + "/guild/v1/get_help_list")
                            .contentType(MediaType.APPLICATION_JSON)
                            .body(BodyInserters.fromObject(this_conext.req))
                            .exchange()
                            .flatMap(res->{
                                switch (res.statusCode()){
                                    case OK:
                                        return res.bodyToMono(res_get_guild_help_list.class);
                                    default:
                                        return res.bodyToMono(String.class).flatMap(str->Mono.error(new Throwable(str)));
                                }
                            });
                })
                .flatMap(this_res_get_guild_help_list->{
                    return Flux.fromIterable(this_res_get_guild_help_list.getOthers())
                            .flatMap(one_res->{
                                game_service_manager.game_service_instance game_instance
                                        = gameServiceManager.cached_instance.getIfPresent(one_res.getPlayer_namespace());
                                if (game_instance == null)
                                    return Mono.error(new Throwable("can not find gameserver"));
                                else
                                    return WebClient.create()
                                            .get()
                                            .uri("http://" + game_instance.host + "/guild/get_member/" + one_res.getPlayer_id())
                                            .exchange()
                                            .flatMap(res->{
                                                switch (res.statusCode()){
                                                    case OK:
                                                        return res.bodyToMono(String.class)
                                                                .flatMap(str->{
                                                                    one_res.setJson_attachment(str);
                                                                    return Mono.just(one_res);
                                                                });
                                                    default:
                                                        return res.bodyToMono(String.class)
                                                                .flatMap(str->{
                                                                    return Mono.just(one_res);
                                                                });
                                                }
                                            });
                            })
                            .count()
                            .map(c->this_res_get_guild_help_list);
                })
                .flatMap(res->{
                    return ServerResponse.ok().contentType(MediaType.APPLICATION_JSON).body(BodyInserters.fromObject(res));
                });
    }
}
