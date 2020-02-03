package com.cyworld.social.utils.request.guild;

import lombok.Data;

@Data
public class req_do_guild_help {
    String player_id;
    String player_namespace;
    String guild_id;
    String target_id;
    String target_namespace;
    String target_help_id;
    int target_help_level;
    long player_coin_reset_timer;
}
