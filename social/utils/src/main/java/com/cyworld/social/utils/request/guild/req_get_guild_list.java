package com.cyworld.social.utils.request.guild;

import lombok.Data;

@Data
public class req_get_guild_list {
    String player_id;
    String player_namespace;
    int skip;
    int take;
    String name;
    String language;
    String domain_id;
    boolean recommend=false;
    int level=0;
}
