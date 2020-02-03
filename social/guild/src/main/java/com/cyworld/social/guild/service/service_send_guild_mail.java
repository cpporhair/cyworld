package com.cyworld.social.guild.service;

import com.cyworld.social.guild.utils.data_service_manager;
import com.cyworld.social.guild.utils.gateway_manager;
import com.cyworld.social.utils.request.guild.req_get_player_pos;
import com.cyworld.social.utils.request.guild.req_new_guild_mail;
import com.cyworld.social.utils.request.mail.req_new_broadcast_mail;
import com.cyworld.social.utils.response.guild.res_get_player_pos;
import lombok.Builder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;
@Component
public class service_send_guild_mail {
    @Builder
    public static class context {
        req_new_guild_mail req;
        req_get_player_pos check_req;
        res_get_player_pos check_res;
        data_service_manager.data_service_instance guild_instance;
        gateway_manager.service_instance send_mail_instance;
    }
    @Autowired
    private data_service_manager dataServiceManager;
    @Autowired
    private gateway_manager gatewayManager;

    public Mono serv(ServerRequest request){
        context this_conext = context
                .builder()
                .guild_instance(dataServiceManager.cached_instance.getIfPresent("guild"))
                .send_mail_instance(gatewayManager.cached_instance.getIfPresent("0"))
                .build();
        return request.bodyToMono(req_new_guild_mail.class)
                .doOnNext(req->{
                    this_conext.req=req;
                    this_conext.check_req=new req_get_player_pos();
                    this_conext.check_req.setGuild_id(req.getGuild_id());
                    this_conext.check_req.setPlayer_id(req.getPlayer_id());
                    this_conext.check_req.setPlayer_namespace(req.getPlayer_namespace());
                })
                .flatMap(req->{
                    return WebClient.create()
                            .post()
                            .uri("http://" + this_conext.guild_instance.host + "/guild/v1/get_player_pos")
                            .contentType(MediaType.APPLICATION_JSON)
                            .body(BodyInserters.fromObject(this_conext.check_req))
                            .exchange()
                            .flatMap(res->{
                                switch (res.statusCode()){
                                    case OK:
                                        return res.bodyToMono(res_get_player_pos.class);
                                    default:
                                        return res.bodyToMono(String.class).flatMap(str->Mono.error(new Throwable(str)));
                                }
                            });
                })
                .flatMap(res->{
                    if(res.getPos()<3)
                        return Mono.error(new Throwable("need pos"));
                    req_new_broadcast_mail mail_req=new req_new_broadcast_mail();
                    mail_req.setId_group("guild.".concat(this_conext.req.getGuild_id()));
                    mail_req.setType(this_conext.req.getType());
                    mail_req.setCondition(this_conext.req.getCondition());
                    mail_req.setAttechmentJson(this_conext.req.getAttechmentJson());
                    mail_req.setFrom(this_conext.req.getFrom());
                    mail_req.setContent(this_conext.req.getContent());
                    mail_req.setTitle(this_conext.req.getTitle());
                    return WebClient.create()
                            .post()
                            .uri("http://" + this_conext.send_mail_instance.host + "/mail/v1/broadcast" )
                            .contentType(MediaType.APPLICATION_JSON)
                            .body(BodyInserters.fromObject(mail_req))
                            .exchange()
                            .flatMap(clientResponse -> {
                                switch (clientResponse.statusCode()){
                                    case OK:
                                        return ServerResponse.ok().body(BodyInserters.fromObject("OK"));
                                    default:
                                        return clientResponse.bodyToMono(String.class).flatMap(str->Mono.error(new Throwable(str)));
                                }
                            });
                });
    }
}
