package com.cyworld.social.utils.response.guild;

import lombok.Data;

@Data
public class res_remove_member {
    public static final int result_ok=0;
    public static final int result_need_pos=1;
    public static final int result_you_are_owner=2;
    public static final int result_not_member=3;
    int result_value;
}
