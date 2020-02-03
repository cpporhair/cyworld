package com.cyworld.social.utils.response.friend;

import lombok.Data;

@Data
public class res_get_my_help {
    String help_type;
    int help_amount;
    long time;
}
