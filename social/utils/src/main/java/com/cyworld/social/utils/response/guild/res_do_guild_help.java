package com.cyworld.social.utils.response.guild;

import lombok.Data;

@Data
public class res_do_guild_help {
    public static final int result_ok=0;
    public static final int unknown_help_id=1;
    public static final int full_count=2;
    public static final int level_unmatched=3;
    public static final int helped=4;
    public static final int not_member=5;
    int result_value;
    long exp;
    boolean coin_able;
}
