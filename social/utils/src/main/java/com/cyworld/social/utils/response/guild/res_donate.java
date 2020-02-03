package com.cyworld.social.utils.response.guild;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
public class res_donate {
    public static final int result_ok=0;
    public static final int result_max_donate=1;
    public static final int result_err_guild_id=2;
    int result_value;
    long old;
    long exp;
}
