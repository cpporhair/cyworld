package com.cyworld.social.test;

import com.cyworld.social.test.simulator.gameserver;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.RouterFunctions;
import org.springframework.web.reactive.function.server.ServerResponse;

@Configuration
public class route_define {
    @Bean
    public RouterFunction<ServerResponse> service_add_value_route_define(gameserver service) {
        return RouterFunctions.route()
                .GET("/friend/get_simple/{id}", service::get_friend_simple)
                .build();
    }
    @Bean
    public RouterFunction<ServerResponse> service_dohelp_route_define(gameserver service) {
        return RouterFunctions.route()
                .GET("/friend/dohelp/{player_id}/{need_id}/{amount}", service::do_help)
                .build();
    }

    @Bean
    public RouterFunction<ServerResponse> service_on_guild_apply_join_route_define(gameserver service) {
        return RouterFunctions.route()
                .GET("/guild/apply_join/{player_id}/{guild_id}", service::on_guild_apply_join)
                .build();
    }
}
