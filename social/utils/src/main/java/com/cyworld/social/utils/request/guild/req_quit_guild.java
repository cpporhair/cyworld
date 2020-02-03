package com.cyworld.social.utils.request.guild;

import lombok.Data;

@Data
public class req_quit_guild {
    String player_id;
    String player_namespace;
    String guild_id;
}
