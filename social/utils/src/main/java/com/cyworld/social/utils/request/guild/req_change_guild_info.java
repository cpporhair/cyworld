package com.cyworld.social.utils.request.guild;

import lombok.Data;

@Data
public class req_change_guild_info {
    String player_namespace;
    String player_id;
    String guild_id;
    String json_value;
}
