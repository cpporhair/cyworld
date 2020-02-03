package com.cyworld.social.utils.response.guild;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
public class res_apply_join {
    public static final int result_ok=0;
    public static final int result_has_joined=1;
    public static final int result_not_owner=2;
    public static final int result_not_asked=3;

    int result_value;
}
