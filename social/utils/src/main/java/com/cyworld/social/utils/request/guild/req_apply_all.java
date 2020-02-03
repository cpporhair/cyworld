package com.cyworld.social.utils.request.guild;

import lombok.Data;

@Data
public class req_apply_all {
    String player_id;
    String player_namespace;
    String guid_id;
    boolean apply;
}
