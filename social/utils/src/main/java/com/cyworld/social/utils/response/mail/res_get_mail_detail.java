package com.cyworld.social.utils.response.mail;

import lombok.Data;

import java.util.List;

@Data
public class res_get_mail_detail {
    String id;
    String read;
    String type;
    String from;
    String time_stamp;
    String title;
    String content;
    List<String> attachmentJson;
}
