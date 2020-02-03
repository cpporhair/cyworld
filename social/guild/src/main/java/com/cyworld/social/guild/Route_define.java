package com.cyworld.social.guild;

import com.cyworld.social.guild.service.*;
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
    public RouterFunction<ServerResponse> route_service_send_guild_mail(service_send_guild_mail service) {
        return RouterFunctions.route()
                .POST("/v1/send_guild_mail", service::serv)
                .build();
    }
    @Bean
    public RouterFunction<ServerResponse> route_service_get_player_guild(service_get_player_guild service) {
        return RouterFunctions.route()
                .POST("/v1/get_player_guild", service::serv)
                .build();
    }
    @Bean
    public RouterFunction<ServerResponse> route_service_drop_guild(service_drop_guild service) {
        return RouterFunctions.route()
                .POST("/v1/drop_guild", service::serv)
                .build();
    }

    @Bean
    public RouterFunction<ServerResponse> route_service_del_guild_help(service_del_guild_help service) {
        return RouterFunctions.route()
                .POST("/v1/del_guild_help", service::serv)
                .build();
    }
    @Bean
    public RouterFunction<ServerResponse> route_service_do_guild_help(service_do_guild_help service) {
        return RouterFunctions.route()
                .POST("/v1/do_guild_help", service::serv)
                .build();
    }
    @Bean
    public RouterFunction<ServerResponse> route_service_get_guild_help_list(service_get_guild_help_list service) {
        return RouterFunctions.route()
                .POST("/v1/get_guild_help_list", service::serv)
                .build();
    }
    @Bean
    public RouterFunction<ServerResponse> route_service_issue_help(service_issue_help service) {
        return RouterFunctions.route()
                .POST("/v1/issue_help", service::serv)
                .build();
    }
    @Bean
    public RouterFunction<ServerResponse> route_service_add_log(service_add_log service) {
        return RouterFunctions.route()
                .POST("/v1/add_log", service::serv)
                .build();
    }
    @Bean
    public RouterFunction<ServerResponse> route_service_remove_member(service_remove_member service) {
        return RouterFunctions.route()
                .POST("/v1/remove_member", service::serv)
                .build();
    }
    @Bean
    public RouterFunction<ServerResponse> route_service_quit_guild(service_quit_guild service) {
        return RouterFunctions.route()
                .POST("/v1/quit_guild", service::serv)
                .build();
    }
    @Bean
    public RouterFunction<ServerResponse> route_service_apply_join(service_apply_join service) {
        return RouterFunctions.route()
                .POST("/v1/apply_join", service::serv)
                .build();
    }
    @Bean
    public RouterFunction<ServerResponse> route_service_ask_join(service_ask_join service) {
        return RouterFunctions.route()
                .POST("/v1/ask_join", service::serv)
                .build();
    }
    @Bean
    public RouterFunction<ServerResponse> route_service_donate(service_donate service) {
        return RouterFunctions.route()
                .POST("/v1/donate", service::serv)
                .build();
    }
    @Bean
    public RouterFunction<ServerResponse> route_service_get_guild_ask_list(service_get_guild_ask_list service) {
        return RouterFunctions.route()
                .POST("/v1/get_guild_ask_list", service::serv)
                .build();
    }
    @Bean
    public RouterFunction<ServerResponse> route_service_get_guild_info(service_get_guild_info service) {
        return RouterFunctions.route()
                .POST("/v1/get_guild_info", service::serv)
                .build();
    }
    @Bean
    public RouterFunction<ServerResponse> route_service_get_guild_list(service_get_guild_list service) {
        return RouterFunctions.route()
                .POST("/v1/get_guild_list", service::serv)
                .build();
    }
    @Bean
    public RouterFunction<ServerResponse> route_service_get_member_detail(service_get_member_detail service) {
        return RouterFunctions.route()
                .POST("/v1/get_member_detail", service::serv)
                .build();
    }
    @Bean
    public RouterFunction<ServerResponse> route_service_get_members(service_get_members service) {
        return RouterFunctions.route()
                .POST("/v1/get_members", service::serv)
                .build();
    }
    @Bean
    public RouterFunction<ServerResponse> route_service_join(service_join service) {
        return RouterFunctions.route()
                .POST("/v1/join", service::serv)
                .build();
    }
    @Bean
    public RouterFunction<ServerResponse> route_service_reject_join(service_reject_join service) {
        return RouterFunctions.route()
                .POST("/v1/reject_join", service::serv)
                .build();
    }
    @Bean
    public RouterFunction<ServerResponse> route_service_change_guild_info(service_change_guild_info service) {
        return RouterFunctions.route()
                .POST("/v1/change_guild_info", service::serv)
                .build();
    }
    @Bean
    public RouterFunction<ServerResponse> route_service_change_owner(service_change_owner service) {
        return RouterFunctions.route()
                .POST("/v1/change_owner", service::serv)
                .build();
    }
    @Bean
    public RouterFunction<ServerResponse> route_service_set_pos(service_set_pos service) {
        return RouterFunctions.route()
                .POST("/v1/set_pos", service::serv)
                .build();
    }
    @Bean
    public RouterFunction<ServerResponse> route_service_update_power(service_update_power service) {
        return RouterFunctions.route()
                .POST("/v1/update_power", service::serv)
                .build();
    }
    @Bean
    public RouterFunction<ServerResponse> route_new_guild(service_new_guild service) {
        return RouterFunctions.route()
                .POST("/v1/new_guild", service::serv)
                .build();
    }
}
