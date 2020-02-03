package com.cyworld.social.utils.request.gameserver;

import lombok.Data;

@Data
public class req_on_guild_dohelp_completed {
    String player_id;
    String player_namespace;
    String guild_id;
    String target_id;
    String target_namespace;
    String target_help_id;
    int target_help_level;
    long current_guild_exp;
}
