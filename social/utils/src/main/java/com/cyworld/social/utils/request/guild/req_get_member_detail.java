package com.cyworld.social.utils.request.guild;

import lombok.Data;

@Data
public class req_get_member_detail {
    String player_id;
    String player_namespace;
    String guild_id;
    long donate_log_reset_expiration;
    long donate_experience_reset_expiration;
}
