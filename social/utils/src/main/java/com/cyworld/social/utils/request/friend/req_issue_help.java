package com.cyworld.social.utils.request.friend;

import lombok.Data;

@Data
public class req_issue_help {
    String player_id;
    String player_namespace;
    String need_ID;
    int amount;
    long lifecycle;
}
