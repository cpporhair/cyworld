package com.cyworld.social.utils.response.guild;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class res_quit_guild {
    public static final int result_ok=0;
    public static final int result_err_guild_id=1;
    public static final int result_you_are_owner=2;
    public static final int result_not_member=3;
    public static final int result_unk=4;
    int result_value;
}
