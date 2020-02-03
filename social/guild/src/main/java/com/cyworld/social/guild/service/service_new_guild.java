package com.cyworld.social.guild.service;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.cyworld.social.guild.utils.Coordinator_service_manager;
import com.cyworld.social.guild.utils.data_service_manager;
import com.cyworld.social.utils.request.global.req_new_server;
import com.cyworld.social.utils.request.guild.req_new_guild;
import com.cyworld.social.utils.request.mail.req_new_subscribe;
import com.cyworld.social.utils.response.guild.res_new_guild;
import com.cyworld.social.utils.transaction.transaction_helper;
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
public class service_new_guild {
    @Builder
    public static class context {
        req_new_guild req;
        res_new_guild res;
        String chat_group_id;
        String mail_group_id;
        String tx_token;
        data_service_manager.data_service_instance guild_instance;
        data_service_manager.data_service_instance mail_instance;
        boolean guild_created=false;
        boolean char_room_created=false;
        boolean transaction_started=false;
    }

    @Autowired
    private data_service_manager dataServiceManager;

    public Mono serv(ServerRequest request){
        context this_conext = context
                .builder()
                .guild_instance(dataServiceManager.cached_instance.getIfPresent("guild"))
                .mail_instance(dataServiceManager.cached_instance.getIfPresent("mail"))
                .tx_token(UUID.randomUUID().toString())
                .build();
        return request.bodyToMono(req_new_guild.class)
                .flatMap(req->{
                    if(req.getName().getBytes().length>18)
                        return Mono.error(new Throwable("name too long"));
                    else
                        return Mono.just(req);
                })
                .doOnNext(req->{
                    this_conext.req=req;
                })
                .flatMap(unused->{
                    return WebClient.create()
                            .post()
                            .uri("http://" + this_conext.guild_instance.host + "/guild/v1/new_guild/")
                            .header("tx_token", this_conext.tx_token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .body(BodyInserters.fromObject(this_conext.req))
                            .exchange()
                            .flatMap(res->{
                                switch (res.statusCode()){
                                    case OK:
                                        this_conext.guild_created=true;
                                        return res.bodyToMono(res_new_guild.class);
                                    default:
                                        return res.bodyToMono(String.class).flatMap(str->Mono.error(new Throwable(str)));
                                }
                            });
                })
                .doOnNext(res->{
                    this_conext.res=res;
                    this_conext.transaction_started=true;
                    this_conext.mail_group_id="guild.".concat(res.getGuild_id());
                })
                .flatMap(str->{
                    req_new_server req_new_mailgroup=new req_new_server();
                    req_new_mailgroup.setDescription("");
                    req_new_mailgroup.setId(this_conext.mail_group_id);
                    return WebClient.create()
                            .post()
                            .uri("http://" + this_conext.mail_instance.host + "/mail/v1/mailgroups")
                            .header("tx_token", this_conext.tx_token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .body(BodyInserters.fromObject(req_new_mailgroup))
                            .exchange();
                })
                .flatMap(res->{
                    switch (res.statusCode()){
                        case OK:
                            return res.bodyToMono(String.class);
                        default:
                            return res.bodyToMono(String.class).flatMap(str->Mono.error(new Throwable(str)));
                    }
                })
                .flatMap(str->{
                    req_new_subscribe next_req = new req_new_subscribe();
                    next_req.setPlayer(this_conext.req.getPlayer_id());
                    next_req.setServer(this_conext.req.getPlayer_namespace());
                    next_req.setGroups(this_conext.mail_group_id);
                    return WebClient.create()
                            .post()
                            .uri("http://" + this_conext.mail_instance.host + "/mail/v1/subscribe")
                            .header("tx_token", this_conext.tx_token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .body(BodyInserters.fromObject(next_req))
                            .exchange();
                })
                .flatMap(res->{
                    switch (res.statusCode()){
                        case OK:
                            return res.bodyToMono(String.class);
                        default:
                            return res.bodyToMono(String.class).flatMap(str->Mono.error(new Throwable(str)));
                    }
                })
                .flatMap(str->{
                    return transaction_helper
                            .commit_all(
                                    Coordinator_service_manager.cached_instance.getIfPresent("transaction").host,
                                    this_conext.tx_token,
                                    this_conext.mail_instance.host,
                                    this_conext.guild_instance.host
                            )
                            .flatMap(res->{
                                if(res)
                                    return Mono.just(res);
                                else
                                    return Mono.error(new Throwable("commit error and undoed"));
                            });
                })
                .doOnError(res->{
                    if(this_conext.transaction_started)
                        transaction_helper
                                .undo_all(
                                        Coordinator_service_manager.cached_instance.getIfPresent("transaction").host,
                                        this_conext.tx_token,
                                        this_conext.mail_instance.host,
                                        this_conext.guild_instance.host
                                )
                                .subscribe();
                })
                .flatMap(b->{
                    return ServerResponse
                            .ok()
                            .contentType(MediaType.APPLICATION_JSON)
                            .body(BodyInserters.fromObject(this_conext.res));
                })
                .log();
    }
}
