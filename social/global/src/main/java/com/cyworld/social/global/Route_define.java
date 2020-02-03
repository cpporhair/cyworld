package com.cyworld.social.global;

import com.cyworld.social.global.service.service_new_gameserver;
import com.cyworld.social.global.service.service_new_user;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.RouterFunctions;
import org.springframework.web.reactive.function.server.ServerResponse;

@Configuration
public class Route_define {
    private static final Logger logger = LoggerFactory.getLogger(Route_define.class.getName());
    @Bean
    public RouterFunction<ServerResponse> new_user_route_define(service_new_user service) {
        return RouterFunctions.route()
                .POST("/v1/user", service::serv)
                .build();
    }

    @Bean
    public RouterFunction<ServerResponse> new_gameserver_route_define(service_new_gameserver service) {
        return RouterFunctions.route()
                .POST("/v1/gameserver", service::serv)
                .build();
    }
}
