package com.cyworld.social.utils.request.guild;

import lombok.Data;

@Data
public class req_apply_join_guild {
    String owner_id;
    String owner_namespace;
    String guid_id;
    String target_id;
    String target_namespace;
}
