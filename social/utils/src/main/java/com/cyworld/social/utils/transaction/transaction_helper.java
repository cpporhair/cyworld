package com.cyworld.social.utils.transaction;

import com.alibaba.fastjson.JSONObject;
import lombok.Builder;
import lombok.Data;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public class transaction_helper {
    @Builder
    @Data
    public static class remote_txc_data{
        String token;
        String host;
    }

    private static Mono<ClientResponse> set_transaction_flag(String coordinator_host,String token,int flag){
        JSONObject json=new JSONObject();
        json.put("uuid",token);
        json.put("status",flag);
        return WebClient.create()
                .post()
                .uri("http://" + coordinator_host + "/transaction/set_transaction_status")
                .contentType(MediaType.APPLICATION_JSON)
                .body(BodyInserters.fromObject(json.toJSONString()))
                .exchange();
    }

    private static Mono<ClientResponse> commit_Mono(String host, String token) {
        return WebClient.create()
                .post()
                .uri("http://" + host + "/inner_data/v1/tx_commit")
                .body(BodyInserters.fromObject(token))
                .exchange();
    }

    private static Mono<ClientResponse> undo_Mono(String host, String token) {
        return WebClient.create()
                .post()
                .uri("http://" + host + "/inner_data/v1/tx_undo")
                .body(BodyInserters.fromObject(token))
                .exchange();
    }
    public static Mono<Long> undo_all(String coordinator_host, String token,String ...hosts){
        return set_transaction_flag(coordinator_host,token,0)
                .flatMapMany(clientResponse -> {
                    switch (clientResponse.statusCode()){
                        case OK:
                        default:
                            return Flux.just(hosts)
                                    .filter(host->host.length()>0)
                                    .flatMap(host->{
                                        return undo_Mono(host,token);
                                    })
                                    .map(c -> {
                                        switch (clientResponse.statusCode()){
                                            case OK:
                                                return Boolean.TRUE;
                                            default:
                                                return Boolean.FALSE;
                                        }
                                    });
                    }
                })
                .count();
    }
    public static Mono<Boolean> commit_all(String coordinator_host, String token,String ...hosts){
        return set_transaction_flag(coordinator_host,token,1)
                .flatMapMany(clientResponse -> {
                    switch (clientResponse.statusCode()){
                        case OK:
                            return Flux.just(hosts)
                                    .filter(host->host.length()>0)
                                    .flatMap(host->{
                                        return commit_Mono(host,token);
                                    });
                        default:
                            return Mono.error(new Throwable("set transaction flag error!"));
                    }
                })
                .map(clientResponse -> {
                    switch (clientResponse.statusCode()){
                        case OK:
                            return Boolean.TRUE;
                        default:
                            return Boolean.FALSE;
                    }
                })
                .all(o->o);
    }
}
