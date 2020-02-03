package com.cyworld.social.guild.service;

import com.alibaba.fastjson.JSON;
import com.cyworld.social.guild.utils.data_service_manager;
import com.cyworld.social.guild.utils.game_service_manager;
import com.cyworld.social.utils.request.guild.req_remove_member;
import com.cyworld.social.utils.request.mail.req_new_subscribe;
import com.cyworld.social.utils.response.common.throwable_res;
import com.cyworld.social.utils.response.guild.res_remove_member;
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
import java.util.UUID;

@Component
public class service_remove_member {
    @Builder
    public static class context {
        req_remove_member req;
        res_remove_member res;
        String chat_group_id;
        String mail_group_id;
        data_service_manager.data_service_instance guild_instance;
        data_service_manager.data_service_instance mail_instance;
        boolean guild_quit=false;
        boolean char_group_quit=false;
        boolean mail_group_quit=false;
    }
    @Autowired
    private data_service_manager dataServiceManager;
    @Autowired
    private game_service_manager gameServiceManager;

    public Mono serv(ServerRequest request){
        context this_conext = context
                .builder()
                .guild_instance(dataServiceManager.cached_instance.getIfPresent("guild"))
                .mail_instance(dataServiceManager.cached_instance.getIfPresent("mail"))
                .build();
        return request.bodyToMono(req_remove_member.class)
                .doOnNext(req->{
                    this_conext.req=req;
                })
                .flatMap(req->{
                    return WebClient.create()
                            .post()
                            .uri("http://" + this_conext.guild_instance.host + "/guild/v1/remove_member")
                            .contentType(MediaType.APPLICATION_JSON)
                            .body(BodyInserters.fromObject(this_conext.req))
                            .exchange()
                            .flatMap(res->{
                                switch (res.statusCode()){
                                    case OK:
                                        return res.bodyToMono(String.class).map(str->{
                                            res_remove_member r= JSON.parseObject(str,res_remove_member.class);
                                            return r;
                                        });
                                    default:
                                        return res.bodyToMono(String.class).flatMap(str->Mono.error(new Throwable(str)));
                                }
                            });
                })
                .flatMap(res->{
                    switch (res.getResult_value()){
                        case res_remove_member.result_ok:
                            this_conext.res=res;
                            this_conext.guild_quit=true;
                            action_on_quit_guild.do_on_quit
                                    (
                                            this_conext.req.getTarget_namespace(),
                                            this_conext.req.getTarget_id(),
                                            this_conext.req.getGuid_id(),
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
                .doOnNext(res->{
                    if(res.getResult_value()!=res_remove_member.result_ok)
                        return;
                    Mono.just(gameServiceManager.cached_instance)
                            .flatMap(instanceCache->{
                                game_service_manager.game_service_instance instance=instanceCache.getIfPresent(this_conext.req.getTarget_namespace());
                                if(instance==null)
                                    return Mono.empty();
                                else
                                    return Mono.just(instance);
                            })
                            .repeatWhenEmpty(10,o->{
                                return o.flatMap(try_times->{
                                    return o.delayElements(Duration.ofSeconds(30*try_times+10));
                                });
                            })
                            .flatMap(game_instance->{
                                return WebClient.create()
                                        .get()
                                        .uri("http://" + game_instance.host + "/guild/quit_guild/"
                                                + this_conext.req.getTarget_id()+"/" + this_conext.req.getGuid_id())
                                        .exchange()
                                        .flatMap(clientResponse -> {
                                            switch (clientResponse.statusCode()){
                                                case OK:
                                                    return clientResponse.bodyToMono(String.class);
                                                case NOT_FOUND:
                                                    return Mono.error(new Throwable());
                                                default:
                                                    return clientResponse.bodyToMono(String.class);
                                            }
                                        })
                                        .retryBackoff(10,Duration.ofSeconds(1));
                            })
                            .subscribe();

                })
                .onErrorResume(e->{
                    if(e instanceof throwable_res){
                        res_remove_member resRemoveMember=new res_remove_member();
                        resRemoveMember.setResult_value(Integer.valueOf(e.getMessage()));
                        return Mono.just(resRemoveMember);
                    }
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
