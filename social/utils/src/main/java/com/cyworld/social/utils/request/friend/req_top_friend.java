package com.cyworld.social.utils.request.friend;

import lombok.Data;

@Data
public class req_top_friend {
    String player_id;
    String player_namespace;
    String friend_id;
    String friend_namespace;
}
