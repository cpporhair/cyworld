package com.cyworld.social.utils.response.friend;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Builder
@Data
@AllArgsConstructor
public class res_one_invite_with_simple_info {
    String friend_id;
    String friend_namespace;
    String type;
    long invite_time;
    String simple_info;
}
