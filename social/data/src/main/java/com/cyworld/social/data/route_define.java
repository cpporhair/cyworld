package com.cyworld.social.data;

import com.cyworld.social.data.service.friends.*;
import com.cyworld.social.data.service.guild.*;
import com.cyworld.social.data.service.guild.srv_guild_issue_help;
import com.cyworld.social.data.service.mail.*;
import com.cyworld.social.data.service.ranks.srv_add_value;
import com.cyworld.social.data.service.ranks.srv_get_ranks;
import com.cyworld.social.data.service.test.srv_clear_all_forTest;
import com.cyworld.social.data.service.transaction.srv_transaction;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.RouterFunctions;
import org.springframework.web.reactive.function.server.ServerResponse;

@Configuration
public class route_define {
    //guild
    @Bean
    public RouterFunction<ServerResponse> srv_change_domain_pos_route_define(srv_change_domain srv) {
        return RouterFunctions.route()
                .POST("/guild/v1/change_domain", srv::serv)
                .build();
    }
    @Bean
    public RouterFunction<ServerResponse> srv_get_player_pos_route_define(srv_get_player_pos srv) {
        return RouterFunctions.route()
                .POST("/guild/v1/get_player_pos", srv::serv)
                .build();
    }
    @Bean
    public RouterFunction<ServerResponse> srv_get_player_guild_route_define(srv_get_player_guild srv) {
        return RouterFunctions.route()
                .POST("/guild/v1/get_player_guild", srv::serv)
                .build();
    }
    @Bean
    public RouterFunction<ServerResponse> srv_drop_guild_route_define(srv_drop_guild srv) {
        return RouterFunctions.route()
                .POST("/guild/v1/drop_guild", srv::serv)
                .build();
    }
    @Bean
    public RouterFunction<ServerResponse> Srv_guild_issue_help_route_define(srv_guild_issue_help srv) {
        return RouterFunctions.route()
                .POST("/guild/v1/guild_issue_help", srv::serv)
                .build();
    }
    @Bean
    public RouterFunction<ServerResponse> srv_get_help_list_route_define(srv_get_help_list srv_get_help_list) {
        return RouterFunctions.route()
                .POST("/guild/v1/get_help_list", srv_get_help_list::serv)
                .build();
    }

    @Bean
    public RouterFunction<ServerResponse> srv_do_guild_help_route_define(srv_do_guild_help srv_do_guild_help) {
        return RouterFunctions.route()
                .POST("/guild/v1/do_guild_help", srv_do_guild_help::serv)
                .build();
    }
    @Bean
    public RouterFunction<ServerResponse> srv_del_guild_help_route_define(srv_del_guild_help srv_del_guild_help) {
        return RouterFunctions.route()
                .POST("/guild/v1/del_guild_help", srv_del_guild_help::serv)
                .build();
    }

    @Bean
    public RouterFunction<ServerResponse> srv_add_log_route_define(srv_add_log srv_add_log) {
        return RouterFunctions.route()
                .POST("/guild/v1/add_log", srv_add_log::serv)
                .build();
    }
    @Bean
    public RouterFunction<ServerResponse> srv_donate_route_define(srv_donate srv_donate) {
        return RouterFunctions.route()
                .POST("/guild/v1/donate", srv_donate::serv)
                .build();
    }
    @Bean
    public RouterFunction<ServerResponse> srv_update_power_route_define(srv_update_power srv_update_power) {
        return RouterFunctions.route()
                .POST("/guild/v1/update_power", srv_update_power::serv)
                .build();
    }
    @Bean
    public RouterFunction<ServerResponse> srv_change_guild_info_route_defin(srv_change_guild_info srv_change_guild_info) {
        return RouterFunctions.route()
                .POST("/guild/v1/change_guild_info", srv_change_guild_info::serv)
                .build();
    }
    @Bean
    public RouterFunction<ServerResponse> srv_change_owner_route_defin(srv_change_owner srv_change_owner) {
        return RouterFunctions.route()
                .POST("/guild/v1/change_owner", srv_change_owner::serv)
                .build();
    }
    @Bean
    public RouterFunction<ServerResponse> srv_set_pos_route_define(srv_set_pos srv_set_pos) {
        return RouterFunctions.route()
                .POST("/guild/v1/set_pos", srv_set_pos::serv)
                .build();
    }
    @Bean
    public RouterFunction<ServerResponse> srv_remove_member_route_define(srv_remove_member srv_remove_member) {
        return RouterFunctions.route()
                .POST("/guild/v1/remove_member", srv_remove_member::serv)
                .build();
    }
    @Bean
    public RouterFunction<ServerResponse> srv_quit_guild_route_define(srv_quit_guild srv_quit_guild) {
        return RouterFunctions.route()
                .POST("/guild/v1/quit_guild", srv_quit_guild::serv)
                .build();
    }
    @Bean
    public RouterFunction<ServerResponse> srv_get_member_detail_route_define(srv_get_member_detail srv_get_member_detail) {
        return RouterFunctions.route()
                .POST("/guild/v1/get_member_detail", srv_get_member_detail::serv)
                .build();
    }
    @Bean
    public RouterFunction<ServerResponse> srv_get_members_route_define(srv_get_members srv_get_members) {
        return RouterFunctions.route()
                .POST("/guild/v1/get_members", srv_get_members::serv)
                .build();
    }
    @Bean
    public RouterFunction<ServerResponse> srv_get_guild_info_route_define(srv_get_guild_info srv_get_guild_info) {
        return RouterFunctions.route()
                .POST("/guild/v1/get_guild_info", srv_get_guild_info::serv)
                .build();
    }
    @Bean
    public RouterFunction<ServerResponse> srv_get_ask_list_for_guild_route_define(srv_get_ask_list_for_guild srv_get_ask_list_for_guild) {
        return RouterFunctions.route()
                .POST("/guild/v1/get_ask_list_for_guild", srv_get_ask_list_for_guild::serv)
                .build();
    }
    @Bean
    public RouterFunction<ServerResponse> srv_get_guild_list_route_define(srv_get_guild_list srv_get_guild_list) {
        return RouterFunctions.route()
                .POST("/guild/v1/get_guild_list", srv_get_guild_list::serv)
                .build();
    }
    @Bean
    public RouterFunction<ServerResponse> srv_reject_join_route_define(srv_reject_join srv_reject_join) {
        return RouterFunctions.route()
                .POST("/guild/v1/reject_join", srv_reject_join::serv)
                .build();
    }
    @Bean
    public RouterFunction<ServerResponse> srv_apply_join_route_define(srv_apply_join srv_apply_join) {
        return RouterFunctions.route()
                .POST("/guild/v1/apply_join", srv_apply_join::serv)
                .build();
    }
    @Bean
    public RouterFunction<ServerResponse> srv_join_route_define(srv_join srv_join) {
        return RouterFunctions.route()
                .POST("/guild/v1/join", srv_join::serv)
                .build();
    }
    @Bean
    public RouterFunction<ServerResponse> srv_ask_join_route_define(srv_ask_join srv_ask_join) {
        return RouterFunctions.route()
                .POST("/guild/v1/ask_join", srv_ask_join::serv)
                .build();
    }
    @Bean
    public RouterFunction<ServerResponse> new_guild_route_define(srv_new_guild srv_new_guild) {
        return RouterFunctions.route()
                .POST("/guild/v1/new_guild", srv_new_guild::serv)
                .build();
    }

    //mail
    @Bean
    public RouterFunction<ServerResponse> new_mailbox_route_define(srv_new_mailbox new_mailbox) {
        return RouterFunctions.route()
                .POST("/mail/v1/users", new_mailbox::serv)
                .build();
    }

    @Bean
    public RouterFunction<ServerResponse> drop_mailgroup_route_define(srv_drop_mailgroup transaction) {
        return RouterFunctions.route()
                .POST("/mail/v1/drop_mailgroups", transaction::serv)
                .build();
    }
    @Bean
    public RouterFunction<ServerResponse> new_mailgroup_route_define(srv_new_mailgroup transaction) {
        return RouterFunctions.route()
                .POST("/mail/v1/mailgroups", transaction::serv)
                .build();
    }

    @Bean
    public RouterFunction<ServerResponse> unsubscribe_route_define(srv_unsubscribe unsubscribe) {
        return RouterFunctions.route()
                .POST("/mail/v1/unsubscribe", unsubscribe::serv)
                .build();
    }

    @Bean
    public RouterFunction<ServerResponse> subscribe_route_define(srv_subscribe subscribe) {
        return RouterFunctions.route()
                .POST("/mail/v1/subscribe", subscribe::serv)
                .build();
    }

    @Bean
    public RouterFunction<ServerResponse> new_del_mail_define(srv_del_mail srv) {
        return RouterFunctions.route()
                .POST("/mail/v1/del_mail", srv::serv)
                .build();
    }

    @Bean
    public RouterFunction<ServerResponse> new_single_mail_route_define(Srv_new_mail_personal new_single_mail) {
        return RouterFunctions.route()
                .POST("/mail/v1/sendto", new_single_mail::serv)
                .build();
    }

    @Bean
    public RouterFunction<ServerResponse> new_broadcast_mail_route_define(srv_new_mail_broadcast new_broadcast_mail) {
        return RouterFunctions.route()
                .POST("/mail/v1/broadcast", new_broadcast_mail::serv)
                .build();
    }

    @Bean
    public RouterFunction<ServerResponse> refresh_mailbox_inbox_route_define(srv_refresh_mailbox_inbox refresh_mailbox_inbox) {
        return RouterFunctions.route()
                .POST("/mail/v1/refresh", refresh_mailbox_inbox::serv)
                .build();
    }

    @Bean
    public RouterFunction<ServerResponse> srv_read_all_mail_route_define(srv_read_all_mail srv_read_all_mail) {
        return RouterFunctions.route()
                .POST("/mail/v1/read_all_mail", srv_read_all_mail::serv)
                .build();
    }

    @Bean
    public RouterFunction<ServerResponse> count_unread_personal_mail_route_define(srv_count_unread_personal_mail srv_count_unread_personal_mail) {
        return RouterFunctions.route()
                .POST("/mail/v1/count_unread_personal_mail", srv_count_unread_personal_mail::serv)
                .build();
    }
    @Bean
    public RouterFunction<ServerResponse> get_mail_list_route_define(Srv_get_mail_list get_mail_list) {
        return RouterFunctions.route()
                .POST("/mail/v1/getmaillist", get_mail_list::serv)
                .build();
    }

    @Bean
    public RouterFunction<ServerResponse> get_mail_detail_route_define(srv_get_mail_detail get_mail_detail) {
        return RouterFunctions.route()
                .POST("/mail/v1/detail", get_mail_detail::serv)
                .build();
    }

    @Bean
    public RouterFunction<ServerResponse> pull_mail_attachment_route_define(srv_pull_mail_attachment pull_mail_attachment) {
        return RouterFunctions.route()
                .POST("/mail/v1/pull_mail_attachment", pull_mail_attachment::serv)
                .build();
    }

    //friend

    @Bean
    public RouterFunction<ServerResponse> srv_top_friend_route_define(srv_top_friend srv_top_friend) {
        return RouterFunctions.route()
                .POST("/friend/v1/top_friend", srv_top_friend::serv)
                .build();
    }

    @Bean
    public RouterFunction<ServerResponse> srv_get_my_help_route_define(srv_get_my_help srv_get_my_help) {
        return RouterFunctions.route()
                .POST("/friend/v1/get_my_help", srv_get_my_help::serv)
                .build();
    }

    @Bean
    public RouterFunction<ServerResponse> srv_issue_help_route_define(srv_issue_help srv_issue_help) {
        return RouterFunctions.route()
                .POST("/friend/v1/issue_help", srv_issue_help::serv)
                .build();
    }

    @Bean
    public RouterFunction<ServerResponse> srv_list_help_route_define(srv_list_help srv_list_help) {
        return RouterFunctions.route()
                .POST("/friend/v1/list_help", srv_list_help::serv)
                .build();
    }

    @Bean
    public RouterFunction<ServerResponse> srv_do_help_route_define(srv_do_help srv_do_help) {
        return RouterFunctions.route()
                .POST("/friend/v1/do_help", srv_do_help::serv)
                .build();
    }

    @Bean
    public RouterFunction<ServerResponse> srv_add_to_blacklist_route_define(srv_add_to_blacklist srv_add_to_blacklist) {
        return RouterFunctions.route()
                .POST("/friend/v1/add_to_blacklist", srv_add_to_blacklist::serv)
                .build();
    }

    @Bean
    public RouterFunction<ServerResponse> srv_apply_route_define(srv_apply srv_apply) {
        return RouterFunctions.route()
                .POST("/friend/v1/apply", srv_apply::serv)
                .build();
    }

    @Bean
    public RouterFunction<ServerResponse> srv_delete_route_define(srv_delete srv_delete) {
        return RouterFunctions.route()
                .POST("/friend/v1/delete", srv_delete::serv)
                .build();
    }

    @Bean
    public RouterFunction<ServerResponse> srv_invite_route_define(srv_invite srv_invite) {
        return RouterFunctions.route()
                .POST("/friend/v1/invite", srv_invite::serv)
                .build();
    }

    @Bean
    public RouterFunction<ServerResponse> srv_list_blacklist_route_define(srv_list_blacklist srv_list_blacklist) {
        return RouterFunctions.route()
                .POST("/friend/v1/list_blacklist", srv_list_blacklist::serv)
                .build();
    }

    @Bean
    public RouterFunction<ServerResponse> srv_list_friends_route_define(srv_list_friends srv_list_friends) {
        return RouterFunctions.route()
                .POST("/friend/v1/list_friends", srv_list_friends::serv)
                .build();
    }

    @Bean
    public RouterFunction<ServerResponse> srv_list_invite_route_define(srv_list_invite srv_list_invite) {
        return RouterFunctions.route()
                .POST("/friend/v1/list_invite", srv_list_invite::serv)
                .build();
    }

    @Bean
    public RouterFunction<ServerResponse> srv_new_player_friends_data_route_define(srv_new_player_friends_data srv_new_player_friends_data) {
        return RouterFunctions.route()
                .POST("/friend/v1/new_player_friends_data", srv_new_player_friends_data::serv)
                .build();
    }

    @Bean
    public RouterFunction<ServerResponse> srv_reject_route_define(srv_reject srv_reject) {
        return RouterFunctions.route()
                .POST("/friend/v1/reject", srv_reject::serv)
                .build();
    }

    @Bean
    public RouterFunction<ServerResponse> srv_remove_from_blacklist_route_define(srv_remove_from_blacklist srv_remove_from_blacklist) {
        return RouterFunctions.route()
                .POST("/friend/v1/remove_from_blacklist", srv_remove_from_blacklist::serv)
                .build();
    }

    //ranks

    @Bean
    public RouterFunction<ServerResponse> srv_add_value_route_define(srv_add_value srv_add_value) {
        return RouterFunctions.route()
                .POST("/rank/v1/add_value", srv_add_value::serv)
                .build();
    }

    @Bean
    public RouterFunction<ServerResponse> srv_get_ranks_route_define(srv_get_ranks srv_get_ranks) {
        return RouterFunctions.route()
                .POST("/rank/v1/get_ranks", srv_get_ranks::serv)
                .build();
    }

    @Bean
    public RouterFunction<ServerResponse> transaction_route_define(srv_transaction transaction) {
        return RouterFunctions.route()
                .POST("/inner_data/v1/tx_commit", transaction::commit)
                .POST("/inner_data/v1/tx_undo", transaction::undo)
                .build();
    }

    @Bean
    public RouterFunction<ServerResponse> test_clear_all_route_define(srv_clear_all_forTest clear_all_forTest) {
        return RouterFunctions.route()
                .GET("/inner_data/v1/clear", clear_all_forTest::serv)
                .build();
    }
}
