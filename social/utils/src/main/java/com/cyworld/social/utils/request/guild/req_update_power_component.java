package com.cyworld.social.utils.request.guild;

import lombok.Data;

@Data
public class req_update_power_component {
    String guild_id;
    String player_id;
    String player_namespace;
    long power;
}
