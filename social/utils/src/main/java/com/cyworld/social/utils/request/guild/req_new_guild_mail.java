package com.cyworld.social.utils.request.guild;

import lombok.Data;

@Data
public class req_new_guild_mail {
    String player_id;
    String player_namespace;
    String guild_id;
    String type;
    String from;
    String title;
    String content;
    String condition;
    String attechmentJson[];
}
