package com.cyworld.social.utils.response.guild;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
public class res_member_manage {
    public static final int result_ok=0;
    public static final int result_err_guild_id=1;
    public static final int result_err_need_pos=2;
    public static final int result_err_not_member=3;
    int result_value;

}
