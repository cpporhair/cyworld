package com.cyworld.social.utils.request.guild;

import lombok.Data;

@Data
public class req_donate {
    String player_id;
    String player_namespace;
    String guid_id;
    String donate_id;
    long mx_donate_count;
    long experience;
    long donate_reset_expiration;
    long experience_reset_expiration;
}
