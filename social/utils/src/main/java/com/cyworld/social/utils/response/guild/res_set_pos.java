package com.cyworld.social.utils.response.guild;

import lombok.Data;

@Data
public class res_set_pos {
    public static final int result_ok=0;
    public static final int result_err_guild_id=1;
    public static final int result_err_need_pos=2;
    public static final int result_err_not_member=3;
    public static final int result_max_vp_count=4;
    public static final int result_max_officer_count=5;
    int result_value;
    int old;
    int pos;
}
