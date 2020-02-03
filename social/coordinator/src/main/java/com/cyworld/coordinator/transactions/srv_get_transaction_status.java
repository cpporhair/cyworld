package com.cyworld.coordinator.transactions;

import com.alibaba.fastjson.JSON;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

@Component
public class srv_get_transaction_status {
    @Autowired
    cached_transactions cached_txc;
    public Mono srv(ServerRequest request){
        return Mono.just(request.pathVariable("uuid"))
                .map(uuid->{
                   transaction txc = cached_txc.cache.getIfPresent(uuid);
                   if(txc==null)
                       return 0;
                   else
                       return txc.status;
                })
                .flatMap(res->{
                    return ServerResponse.ok().contentType(MediaType.APPLICATION_JSON).body(BodyInserters.fromObject(res.toString()));
                });
    }
}
