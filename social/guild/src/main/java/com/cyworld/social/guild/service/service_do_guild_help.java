package com.cyworld.social.guild.service;

import com.cyworld.social.guild.utils.data_service_manager;
import com.cyworld.social.guild.utils.game_service_manager;
import com.cyworld.social.utils.request.gameserver.req_on_guild_dohelp_completed;
import com.cyworld.social.utils.request.guild.req_do_guild_help;
import com.cyworld.social.utils.response.guild.res_do_guild_help;
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
public class service_do_guild_help {
    @Builder
    public static class context {
        req_do_guild_help req;
        res_do_guild_help res;
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
        return request.bodyToMono(req_do_guild_help.class)
                .doOnNext(req->{
                    this_conext.req=req;
                })
                .flatMap(req->{
                    return WebClient.create()
                            .post()
                            .uri("http://" + this_conext.guild_instance.host + "/guild/v1/do_guild_help")
                            .contentType(MediaType.APPLICATION_JSON)
                            .body(BodyInserters.fromObject(this_conext.req))
                            .exchange()
                            .flatMap(res->{
                                switch (res.statusCode()){
                                    case OK:
                                        return res.bodyToMono(res_do_guild_help.class);
                                    default:
                                        return res.bodyToMono(String.class).flatMap(str->Mono.error(new Throwable(str)));
                                }
                            });
                })
                .doOnNext(res->{
                    if(res.getResult_value()!=res_do_guild_help.result_ok)
                        return;
                    req_on_guild_dohelp_completed game_req=new req_on_guild_dohelp_completed();
                    game_req.setCurrent_guild_exp(res.getExp());
                    game_req.setGuild_id(this_conext.req.getGuild_id());
                    game_req.setPlayer_id(this_conext.req.getPlayer_id());
                    game_req.setPlayer_namespace(this_conext.req.getPlayer_namespace());
                    game_req.setTarget_help_id(this_conext.req.getTarget_help_id());
                    game_req.setTarget_help_level(this_conext.req.getTarget_help_level());
                    game_req.setTarget_id(this_conext.req.getTarget_id());
                    game_req.setTarget_namespace(this_conext.req.getTarget_namespace());
                    game_service_manager.game_service_instance game_instance
                            = gameServiceManager.cached_instance.getIfPresent(this_conext.req.getTarget_namespace());
                    WebClient.create()
                            .post()
                            .uri("http://" + game_instance.host + "/guild/do_guild_help/")
                            .contentType(MediaType.APPLICATION_JSON)
                            .body(BodyInserters.fromObject(game_req))
                            .exchange()
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
                })
                .flatMap(res->{
                    return ServerResponse.ok()
                            .contentType(MediaType.APPLICATION_JSON)
                            .body(BodyInserters.fromObject(res));
                });
    }
}
