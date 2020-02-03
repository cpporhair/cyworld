package com.cyworld.social.guild.utils;

import com.alibaba.nacos.api.naming.listener.NamingEvent;
import com.cyworld.social.utils.nacos.naming_listener;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import lombok.Builder;
import lombok.Data;
import lombok.Value;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

import javax.annotation.PostConstruct;

@Component
public class gateway_manager {
    @Autowired
    private Environment env;

    @Value
    public static class service_instance {
        public long id;
        public String host;
        public boolean online;
    }
    public Cache<String, service_instance> cached_instance = Caffeine.newBuilder()
            .maximumSize(1000).build();
    @PostConstruct
    private void init() throws Exception {
        naming_listener.start_listener(
                env.getProperty("spring.cloud.nacos.discovery.server-addr"),
                "GatewayService",
                e -> {
                    cached_instance.invalidateAll();
                    Flux.fromIterable(((NamingEvent) e).getInstances())
                            .index()
                            .map(t -> {
                                return new service_instance(
                                        t.getT1(),
                                        t.getT2().getIp()+ ":" +t.getT2().getPort(),
                                        t.getT2().isHealthy());
                            })
                            .subscribe(o -> {
                                if(o.online)
                                    cached_instance.put(String.valueOf(o.id), o);
                            });
                }
        );
    }
}
