package com.cyworld.social.utils.response.guild;

import lombok.Data;

@Data
public class res_issue_help {
    public static final int result_ok=0;
    public static final int result_has_same=1;
    public static final int result_not_member=1;
    int result_value;
}
