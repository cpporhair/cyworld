package com.cyworld.social.guild.service;

import com.cyworld.social.utils.request.mail.req_new_subscribe;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.BitSet;

public class action_on_join_guild {

    public static final int mail_group_joined=2;

    public static Mono<BitSet> build_mono(
            String mail_instance_host,
            String tx_token,
            String player_namespace,
            String player_id,
            String guild_id){
        return Mono.just(new BitSet())
                .flatMap(bi->{
                    req_new_subscribe next_req = new req_new_subscribe();
                    next_req.setPlayer(player_id);
                    next_req.setServer(player_namespace);
                    next_req.setGroups("guild.".concat(guild_id));
                    return WebClient.create()
                            .post()
                            .uri("http://" + mail_instance_host + "/mail/v1/subscribe")
                            .header("tx_token", tx_token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .body(BodyInserters.fromObject(next_req))
                            .exchange()
                            .flatMap(clientResponse -> {
                                switch (clientResponse.statusCode()){
                                    case OK:
                                        bi.set(mail_group_joined);
                                        return clientResponse.bodyToMono(String.class).map(s->bi);
                                    default:
                                        return clientResponse.bodyToMono(String.class).flatMap(e->Mono.error(new Throwable(e)));
                                }
                            });
                });

    }
}
