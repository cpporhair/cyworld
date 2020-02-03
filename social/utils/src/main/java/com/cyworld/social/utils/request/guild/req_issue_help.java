package com.cyworld.social.utils.request.guild;

import lombok.Data;

import java.util.stream.Stream;

@Data
public class req_issue_help {
    String player_id;
    String player_namespace;
    String guild_id;
    String help_id;
    int help_level;
    int max_help_count;
    int max_coin_count;
}
