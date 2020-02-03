package com.cyworld.social.global.utils;

import com.alibaba.nacos.api.naming.listener.NamingEvent;
import com.cyworld.social.utils.nacos.naming_listener;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import lombok.Builder;
import lombok.Data;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

import javax.annotation.PostConstruct;

@Component
public class data_service_manager {

    @Autowired
    private Environment env;

    @Data
    @Builder
    public static class data_service_instance {
        public String db_name;
        public String host;
        public boolean online;
    }


    public Cache<String, data_service_instance> cached_instance = Caffeine.newBuilder()
            .maximumSize(1000).build();

    @PostConstruct
    private void init() throws Exception {
        naming_listener.start_listener(
                env.getProperty("spring.cloud.nacos.discovery.server-addr"),
                "DataService",
                e -> {
                    cached_instance.invalidateAll();
                    Flux.fromIterable(((NamingEvent) e).getInstances())
                            .flatMap(i -> {
                                return Flux.fromArray(i.getMetadata().get("dataKey").split(";"))
                                        .map(s -> data_service_instance
                                                .builder()
                                                .db_name(s)
                                                .host(i.getIp() + ":" + i.getPort())
                                                .online(i.isHealthy())
                                                .build());
                            })
                            .subscribe(o -> {
                                cached_instance.put(o.db_name, o);
                            });
                }
        );
    }
}
