package com.cyworld.social.guild.service;

import com.alibaba.fastjson.JSON;
import com.cyworld.social.guild.utils.Coordinator_service_manager;
import com.cyworld.social.guild.utils.data_service_manager;
import com.cyworld.social.utils.request.guild.req_join;
import com.cyworld.social.utils.response.common.throwable_res;
import com.cyworld.social.utils.response.guild.res_join;
import com.cyworld.social.utils.transaction.transaction_helper;
import lombok.Builder;
import org.apache.logging.log4j.util.Strings;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

import java.util.BitSet;
import java.util.UUID;
@Component
public class service_join {

    @Builder
    public static class context {
        req_join req;
        res_join res;
        String mail_group_id;
        String tx_token;
        data_service_manager.data_service_instance guild_instance;
        data_service_manager.data_service_instance mail_instance;
        boolean guild_joined=false;
        boolean mail_group_joined=false;
        boolean transaction_started=false;
    }

    @Autowired
    private data_service_manager dataServiceManager;
    private Mono<BitSet> do_on_joined(context this_context){
        return action_on_join_guild.build_mono
                (
                        this_context.mail_instance.host,
                        this_context.tx_token,
                        this_context.req.getPlayer_namespace(),
                        this_context.req.getPlayer_id(),
                        this_context.req.getGuild_id()
                )
                .doOnNext(bi->{
                            this_context.mail_group_joined=bi.get(action_on_join_guild.mail_group_joined);
                        }
                );
    }

    public Mono serv(ServerRequest request){
        context this_conext = context
                .builder()
                .guild_instance(dataServiceManager.cached_instance.getIfPresent("guild"))
                .mail_instance(dataServiceManager.cached_instance.getIfPresent("mail"))
                .tx_token(UUID.randomUUID().toString())
                .build();
        return request.bodyToMono(req_join.class)
                .doOnNext(req->{
                    this_conext.req=req;
                })
                .flatMap(req->{
                    return WebClient.create()
                            .post()
                            .uri("http://" + this_conext.guild_instance.host + "/guild/v1/join")
                            .header("tx_token", this_conext.tx_token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .body(BodyInserters.fromObject(this_conext.req))
                            .exchange()
                            .flatMap(res->{
                                switch (res.statusCode()){
                                    case OK:
                                        return res.bodyToMono(String.class).map(str->{
                                            res_join r=JSON.parseObject(str,res_join.class);
                                            return r;
                                        });
                                    default:
                                        return res.bodyToMono(String.class).flatMap(str->Mono.error(new Throwable(str)));
                                }
                            });
                })
                .flatMap(res->{
                    switch (res.getResultValue()){
                        case res_join.result_ok:
                            this_conext.res=res;
                            this_conext.guild_joined=true;
                            this_conext.transaction_started=true;
                            return do_on_joined(this_conext);
                        default:
                            return Mono.error(new throwable_res(String.valueOf(res.getResultValue())));
                    }
                })
                .map(str->{
                    return this_conext.res;
                })
                .doOnNext(res->{
                    transaction_helper
                            .commit_all(
                                    Coordinator_service_manager.cached_instance.getIfPresent("transaction").host,
                                    this_conext.tx_token,
                                    this_conext.mail_instance.host,
                                    this_conext.guild_instance.host
                            ).subscribe();
                })
                .doOnError(e->{
                    if(this_conext.transaction_started)
                        transaction_helper
                                .undo_all(
                                        Coordinator_service_manager.cached_instance.getIfPresent("transaction").host,
                                        this_conext.guild_joined?this_conext.guild_instance.host: Strings.EMPTY,
                                        this_conext.mail_group_joined?this_conext.mail_instance.host:Strings.EMPTY
                                ).subscribe();

                })
                .onErrorResume(e->{
                    if(e instanceof throwable_res)
                        return Mono.just(new res_join(Integer.valueOf(e.getMessage())));
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
