package com.cyworld.social.utils.request.guild;

import lombok.Data;

@Data
public class req_get_guild_info {
    String player_id;
    String player_namespace;
    String guild_id;
}
