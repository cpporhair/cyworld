package com.cyworld.social.utils.request.mail;

import lombok.Data;

@Data
public class req_new_personal_mail {
    String id_box;
    String id_namespace;
    String type;
    String from;
    String title;
    String content;
    String attechmentJson[];
}
