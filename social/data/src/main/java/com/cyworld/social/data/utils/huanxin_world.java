package com.cyworld.social.data.utils;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;

@Component
public class huanxin_world {
    @Autowired
    com.cyworld.social.data.actors.akka_system akka_system;
    @Autowired
    Environment environment;
    @Autowired
    ApplicationContext application_context;
    public String huanxin_base_path;

    @PostConstruct
    public void init() throws Exception {
        huanxin_base_path = environment.getProperty("huanxin_basePath");
        /*
        Flux.interval(Duration.ZERO,Duration.ofSeconds(600))
                .repeat()
                .flatMap(i->{
                    return huanxin_token_getter
                            .build_mono
                                    (
                                            environment.getProperty("huanxin_basePath"),
                                            environment.getProperty("huanxin_grant_type"),
                                            environment.getProperty("huanxin_client_id"),
                                            environment.getProperty("huanxin_client_secret")
                                    )
                            .doOnNext(t->{
                                huanxin_token_getter.huanxin_token.ar.set(t);
                                actor_chatrooms.find_or_load(huanxin_base_path,akka_system.actor_system);
                            })
                            .onErrorContinue((e,o)->{
                                e.printStackTrace();
                            });
                })
                .subscribe();
       */
    }
}
