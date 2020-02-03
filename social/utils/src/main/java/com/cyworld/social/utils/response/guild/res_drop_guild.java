package com.cyworld.social.utils.response.guild;

import lombok.Data;

@Data
public class res_drop_guild {
    public static final int result_ok=0;
    public static final int not_empty=1;
    public static final int not_owner=2;
    int result_value;
}
