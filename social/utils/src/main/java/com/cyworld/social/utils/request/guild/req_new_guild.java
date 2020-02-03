package com.cyworld.social.utils.request.guild;

import lombok.Data;

@Data
public class req_new_guild {
    String player_id;
    String player_namespace;
    String name;
    String language;
    String declaration;
    String flag;
    String domain_id;
    String notice;
    boolean need_apply_when_join;
    int level_for_join;
    int max_member_count;
    long owner_power;
}
