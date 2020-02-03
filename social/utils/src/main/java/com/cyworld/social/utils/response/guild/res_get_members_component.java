package com.cyworld.social.utils.response.guild;

import lombok.Data;

@Data
public class res_get_members_component {
    String id;
    String namespace;
    String pos;
    long power;
    long exp;
    String donate;
    String member_attachment_json;
}
