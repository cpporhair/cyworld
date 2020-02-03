package com.cyworld.social.utils.request.guild;

import lombok.Data;

@Data
public class req_del_guild_help {
    String target_id;
    String target_namespace;
    String target_help_id;
    String guild_id;
}
