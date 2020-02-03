package com.cyworld.coordinator;

import com.cyworld.coordinator.transactions.srv_get_transaction_status;
import com.cyworld.coordinator.transactions.srv_set_transaction_status;
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
    public RouterFunction<ServerResponse> new_user_route_define(srv_get_transaction_status service) {
        return RouterFunctions.route()
                .GET("/transaction/get_transaction_status/{uuid}", service::srv)
                .build();
    }

    @Bean
    public RouterFunction<ServerResponse> new_gameserver_route_define(srv_set_transaction_status service) {
        return RouterFunctions.route()
                .POST("/transaction/set_transaction_status", service::srv)
                .build();
    }
}
