package com.cyworld.social.utils.request.guild;

import lombok.Data;

@Data
public class req_ask_join {
    String player_id;
    String player_namespace;
    String guild_id;
    int level;
    long power;
}
