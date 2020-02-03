package com.cyworld.social.utils.request.mail;

import lombok.Data;

@Data
public class req_new_broadcast_mail {
    String id_group;
    String type;
    String from;
    String title;
    String content;
    String condition;
    String attechmentJson[];
}
