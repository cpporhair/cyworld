package com.cyworld.social.guild.service;

import com.cyworld.social.utils.request.mail.req_new_subscribe;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Duration;

public class action_on_quit_guild {
    public static void do_on_quit(
            String player_namespace,
            String player_id,
            String guild_id,
            String mail_instance_host){

        req_new_subscribe next_req = new req_new_subscribe();
        next_req.setPlayer(player_id);
        next_req.setServer(player_namespace);
        next_req.setGroups("guild.".concat(guild_id));
        WebClient.create()
                .post()
                .uri("http://" + mail_instance_host + "/mail/v1/unsubscribe")
                .contentType(MediaType.APPLICATION_JSON)
                .body(BodyInserters.fromObject(next_req))
                .exchange()
                //.subscribeOn(Schedulers.fromExecutor(Executors.newSingleThreadExecutor()))
                .flatMap(clientResponse -> {
                    switch (clientResponse.statusCode()){
                        case OK:
                            return Mono.just(1);
                        default:
                            return Mono.error(new Throwable());
                    }
                })
                .retryBackoff(20, Duration.ofSeconds(30))
                .log()
                .subscribe();
    }
}
