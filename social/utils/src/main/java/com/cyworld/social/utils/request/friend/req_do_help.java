package com.cyworld.social.utils.request.friend;

import lombok.Data;

@Data
public class req_do_help {
    String player_id;
    String player_namespace;
    String target_id;
    String target_namespace;
    String need_ID;
    int amount;
}
