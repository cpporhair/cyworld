package com.cyworld.social.utils.request.guild;

import lombok.Data;

@Data
public class req_change_owner {
    String player_id;
    String player_namespace;
    String guid_id;
    String target_id;
    String target_namespace;
}
