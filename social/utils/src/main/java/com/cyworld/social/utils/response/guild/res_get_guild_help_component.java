package com.cyworld.social.utils.response.guild;

import lombok.Data;

@Data
public class res_get_guild_help_component {
    String player_id;
    String player_namespace;
    String json_attachment;
    String help_id;
    int help_count;
    int max_help_count;
}
