package com.cyworld.social.friends;

import com.cyworld.social.friends.service.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.RouterFunctions;
import org.springframework.web.reactive.function.server.ServerResponse;

@Configuration
public class friends_route {

    @Bean
    public RouterFunction<ServerResponse> srv_delete_route_define(service_delete service_delete) {
        return RouterFunctions.route()
                .POST("/v1/del_friend", service_delete::serv)
                .build();
    }

    @Bean
    public RouterFunction<ServerResponse> srv_top_friend_route_define(service_top_friend service_top_friend) {
        return RouterFunctions.route()
                .POST("/v1/top_friend", service_top_friend::serv)
                .build();
    }

    @Bean
    public RouterFunction<ServerResponse> srv_get_my_help_route_define(service_get_my_help service_get_my_help) {
        return RouterFunctions.route()
                .POST("/v1/get_my_help", service_get_my_help::serv)
                .build();
    }

    @Bean
    public RouterFunction<ServerResponse> srv_issue_help_route_define(service_issue_help srv_issue_help) {
        return RouterFunctions.route()
                .POST("/v1/issue_help", srv_issue_help::serv)
                .build();
    }

    @Bean
    public RouterFunction<ServerResponse> srv_list_help_route_define(service_list_help srv_list_help) {
        return RouterFunctions.route()
                .POST("/v1/list_help", srv_list_help::serv)
                .build();
    }

    @Bean
    public RouterFunction<ServerResponse> srv_do_help_route_define(service_do_help srv_do_help) {
        return RouterFunctions.route()
                .POST("/v1/do_help", srv_do_help::serv)
                .build();
    }

    @Bean
    public RouterFunction<ServerResponse> service_add_to_blacklist_route_define(service_add_to_blacklist service) {
        return RouterFunctions.route()
                .POST("/v1/add_to_blacklist", service::serv)
                .build();
    }

    @Bean
    public RouterFunction<ServerResponse> service_apply_route_define(service_apply service) {
        return RouterFunctions.route()
                .POST("/v1/apply", service::serv)
                .build();
    }

    @Bean
    public RouterFunction<ServerResponse> service_get_blacklist_route_define(service_get_blacklist service) {
        return RouterFunctions.route()
                .POST("/v1/get_blacklist", service::serv)
                .build();
    }

    @Bean
    public RouterFunction<ServerResponse> service_invite_route_define(service_invite service) {
        return RouterFunctions.route()
                .POST("/v1/invite", service::serv)
                .build();
    }

    @Bean
    public RouterFunction<ServerResponse> service_list_friends_route_define(service_list_friends service) {
        return RouterFunctions.route()
                .POST("/v1/list_friends", service::serv)
                .build();
    }

    @Bean
    public RouterFunction<ServerResponse> service_list_invite_route_define(service_list_invite service) {
        return RouterFunctions.route()
                .POST("/v1/list_invite", service::serv)
                .build();
    }

    @Bean
    public RouterFunction<ServerResponse> service_reject_route_define(service_reject service) {
        return RouterFunctions.route()
                .POST("/v1/reject", service::serv)
                .build();
    }

    @Bean
    public RouterFunction<ServerResponse> service_remove_from_blacklist_route_define(service_remove_from_blacklist service) {
        return RouterFunctions.route()
                .POST("/v1/remove_from_blacklist", service::serv)
                .build();
    }

}
