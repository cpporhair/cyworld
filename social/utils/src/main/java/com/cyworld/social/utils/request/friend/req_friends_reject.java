package com.cyworld.social.utils.request.friend;

import lombok.Data;

@Data
public class req_friends_reject {
    String player_id;
    String player_namespace;
    String friend_id;
    String friend_namespace;
}
