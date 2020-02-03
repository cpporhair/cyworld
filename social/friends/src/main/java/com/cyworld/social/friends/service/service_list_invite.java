package com.cyworld.social.friends.service;

import com.alibaba.fastjson.JSONObject;
import com.cyworld.social.friends.utils.data_service_manager;
import com.cyworld.social.friends.utils.game_service_manager;
import com.cyworld.social.utils.request.friend.req_list_invite;
import com.cyworld.social.utils.response.friend.res_one_invite_with_simple_info;
import com.cyworld.social.utils.response.friend.res_one_invite;
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
public class service_list_invite {
    @Builder
    public static class context {
        req_list_invite req;
        data_service_manager.data_service_instance instance;
    }

    @Autowired
    private data_service_manager dataServiceManager;
    @Autowired
    private game_service_manager gameServiceManager;

    @SuppressWarnings("unchecked")
    public Mono serv(ServerRequest request) {
        context this_conext = context
                .builder()
                .instance(dataServiceManager.cached_instance.getIfPresent("friend"))
                .build();
        return request.bodyToMono(req_list_invite.class)
                .doOnNext(body -> {
                    this_conext.req = body;
                })
                .flatMap(body -> {
                    return WebClient.create()
                            .post()
                            .uri("http://" + this_conext.instance.host + "/friend/v1/list_invite")
                            .contentType(MediaType.APPLICATION_JSON)
                            .body(BodyInserters.fromObject(body))
                            .exchange();
                })
                .flatMap(res -> {
                    switch (res.statusCode()) {
                        case OK:
                            return res.bodyToMono(List.class);
                        default:
                            return res.bodyToMono(String.class).flatMap(t -> Mono.error(new Throwable(t)));
                    }
                })

                .flatMap(lst->{
                    return Flux.fromIterable(lst)
                            .map(o->{
                                return JSONObject.parseObject(o.toString(),res_one_invite.class);
                            })
                            //.parallel()
                            .flatMap(obj->{
                                res_one_invite invite_msg = res_one_invite.class.cast(obj);
                                game_service_manager.game_service_instance instance
                                        =gameServiceManager.cached_instance.getIfPresent(invite_msg.getFrom_namespace());
                                if(instance==null)
                                    return Mono.error(new Throwable("can not find gameserver"));
                                else
                                    return WebClient.create()
                                            .get()
                                            .uri("http://" + instance.host + "/friend/get_simple/" + invite_msg.getFrom_id())
                                            .exchange()
                                            .flatMap(res->{
                                                switch (res.statusCode()){
                                                    case OK:
                                                        return res.bodyToMono(String.class)
                                                                .map(s->{
                                                                    return res_one_invite_with_simple_info
                                                                            .builder()
                                                                            .friend_id(invite_msg.getFrom_id())
                                                                            .friend_namespace(invite_msg.getFrom_namespace())
                                                                            .type(invite_msg.getType())
                                                                            .invite_time(invite_msg.getInvite_time())
                                                                            .simple_info(s)
                                                                            .build();
                                                                })
                                                                .map(o->{
                                                                    return JSONObject.toJSON(o).toString();
                                                                });
                                                    default:
                                                        return Mono.just(res_one_invite_with_simple_info
                                                                .builder()
                                                                .friend_id(invite_msg.getFrom_id())
                                                                .friend_namespace(invite_msg.getFrom_namespace())
                                                                .type(invite_msg.getType())
                                                                .simple_info("NONE")
                                                                .build())
                                                                .map(o->{
                                                                    return JSONObject.toJSON(o).toString();
                                                                });
                                                }
                                            });
                            })
                            //.sequential()
                            .collectList();
                })
                .flatMap(res->{
                    return ServerResponse.ok().contentType(MediaType.APPLICATION_JSON).body(BodyInserters.fromObject(res));
                });
    }
}
