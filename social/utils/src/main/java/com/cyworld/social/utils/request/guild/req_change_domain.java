package com.cyworld.social.utils.request.guild;

import lombok.Data;

@Data
public class req_change_domain {
    int change_type;
    String change_id;
    String new_domain;
}
