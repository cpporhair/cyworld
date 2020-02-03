package com.cyworld.social.utils.request.guild;

import lombok.Data;

@Data
public class req_add_log {
    String guild_id;
    String log;
    int max_log_count;
}
