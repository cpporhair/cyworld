package com.cyworld.social.utils.request.friend;

import lombok.Data;

@Data
public class req_friends_invite {
    String from_id;
    String from_namespace;
    String target_id;
    String target_namespace;
}
