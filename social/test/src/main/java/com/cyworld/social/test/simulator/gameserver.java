package com.cyworld.social.test.simulator;

import com.alibaba.fastjson.JSON;
import com.cyworld.social.utils.response.friend.res_one_friend_with_simple_info;
import lombok.Builder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

@Component
public class gameserver {
    private static final Logger logger = LoggerFactory.getLogger(gameserver.class.getName());
    public Mono get_friend_simple(ServerRequest request){
        return Mono.just(request.pathVariable("id"))
                .map(s->{
                    return JSON.toJSONString(res_one_friend_with_simple_info.builder()
                            ._id("1").namespace("2").simple_info("3").build());
                })
                .flatMap(res->{
                    return ServerResponse.ok().contentType(MediaType.APPLICATION_JSON).body(BodyInserters.fromObject(res));
                });
    }

    public Mono do_help(ServerRequest request){
        return Mono.just(request)
                .doOnNext(req->{
                    logger.info(req.pathVariable("player_id")
                            .concat(".")
                            .concat(req.pathVariable("need_id"))
                            .concat(".")
                            .concat(req.pathVariable("amount")));
                })
                .flatMap(res->{
                    return ServerResponse.ok().contentType(MediaType.APPLICATION_JSON).body(BodyInserters.fromObject("OK"));
                });
    }

    public Mono on_guild_apply_join(ServerRequest request){
        return ServerResponse.ok().contentType(MediaType.APPLICATION_JSON).body(BodyInserters.fromObject("OK"));
    }
}
