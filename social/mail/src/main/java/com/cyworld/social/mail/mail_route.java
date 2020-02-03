package com.cyworld.social.mail;

import com.cyworld.social.mail.service.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.RouterFunctions;
import org.springframework.web.reactive.function.server.ServerResponse;

@Configuration
public class mail_route {
    @Bean
    public RouterFunction<ServerResponse> service_read_all_mail_route_define(service_read_all_mail service) {
        return RouterFunctions.route()
                .POST("/v1/read_all_mail", service::serv)
                .build();
    }

    @Bean
    public RouterFunction<ServerResponse> del_mail_route_define(service_del_mail service) {
        return RouterFunctions.route()
                .POST("/v1/del_mail", service::serv)
                .build();
    }
    @Bean
    public RouterFunction<ServerResponse> subscribe_route_define(service_subscribe service) {
        return RouterFunctions.route()
                .POST("/v1/subscribe", service::serv)
                .build();
    }

    @Bean
    public RouterFunction<ServerResponse> sendto_route_define(service_new_personal_mail service) {
        return RouterFunctions.route()
                .POST("/v1/sendto", service::serv)
                .build();
    }

    @Bean
    public RouterFunction<ServerResponse> broadcast_route_define(service_new_broadcast_mail service) {
        return RouterFunctions.route()
                .POST("/v1/broadcast", service::serv)
                .build();
    }

    @Bean
    public RouterFunction<ServerResponse> service_count_unread_personal_mail_route_define(service_count_unread_personal_mail service) {
        return RouterFunctions.route()
                .POST("/v1/count_unread_personal_mail", service::serv)
                .build();
    }

    @Bean
    public RouterFunction<ServerResponse> get_mail_list_route_define(service_get_mail_list service) {
        return RouterFunctions.route()
                .POST("/v1/getmaillist", service::serv)
                .build();
    }

    @Bean
    public RouterFunction<ServerResponse> get_mail_detail_route_define(service_get_mail_detail service) {
        return RouterFunctions.route()
                .POST("/v1/detail", service::serv)
                .build();
    }

    @Bean
    public RouterFunction<ServerResponse> pull_mail_attachment_route_define(service_pull_mail_attachment service) {
        return RouterFunctions.route()
                .POST("/v1/pull_mail_attachment", service::serv)
                .build();
    }
}
