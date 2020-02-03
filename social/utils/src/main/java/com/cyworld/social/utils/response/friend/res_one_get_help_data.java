package com.cyworld.social.utils.response.friend;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Builder
@Data
@AllArgsConstructor
public class res_one_get_help_data {
    String frined_id;
    String friend_namespace;
    String help_type;
    String simple_info;
    int help_amount;
}