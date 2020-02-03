package com.cyworld.coordinator.transactions;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

@Component
public class srv_set_transaction_status {
    @Autowired
    cached_transactions cached_txc;
    public Mono srv(ServerRequest request){
        return request.bodyToMono(String.class)
                .flatMap(json->{
                    JSONObject o=JSON.parseObject(json);
                    String uuid=o.getString("uuid");
                    Integer status=o.getInteger("status");
                    cached_txc.cache.put(uuid,transaction.builder().uuid(uuid).status(status).build());
                    return ServerResponse.ok().body(BodyInserters.fromObject("OK"));
                });
    }
}
