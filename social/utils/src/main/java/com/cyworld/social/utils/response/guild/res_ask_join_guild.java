package com.cyworld.social.utils.response.guild;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class res_ask_join_guild {
    public static final int result_ok=0;
    public static final int result_max_count=1;
    public static final int result_level_mismatching=2;
    public static final int result_needent=3;
    public static final int result_has_joined=4;
    public static final int result_has_asked=5;
    public static final int result_quit_in_24=6;

    int result_value;
    String chat_group_id;
}
