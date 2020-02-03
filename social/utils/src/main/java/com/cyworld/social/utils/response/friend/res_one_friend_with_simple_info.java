package com.cyworld.social.utils.response.friend;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@AllArgsConstructor
public class res_one_friend_with_simple_info {
    String _id;
    String namespace;
    int status;
    String simple_info;
}
