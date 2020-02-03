package com.cyworld.social.utils.request.mail;

import lombok.Data;

@Data
public class req_get_mail_list {
    String id;
    String namesapce;
    String type;
    String skip;
    String take;
}
