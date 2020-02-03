package com.cyworld.social.ranks;

import com.cyworld.social.ranks.service.service_add_value;
import com.cyworld.social.ranks.service.service_get_ranks;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.RouterFunctions;
import org.springframework.web.reactive.function.server.ServerResponse;

@Configuration
public class ranks_route {
    @Bean
    public RouterFunction<ServerResponse> service_add_value_route_define(service_add_value service) {
        return RouterFunctions.route()
                .POST("/v1/add_value", service::serv)
                .build();
    }

    @Bean
    public RouterFunction<ServerResponse> service_get_ranks_route_define(service_get_ranks service) {
        return RouterFunctions.route()
                .POST("/v1/get_ranks", service::serv)
                .build();
    }


}
