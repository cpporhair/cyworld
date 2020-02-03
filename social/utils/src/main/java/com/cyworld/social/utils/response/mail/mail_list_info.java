package com.cyworld.social.utils.response.mail;

import lombok.Data;

@Data
public class mail_list_info {
    String id;
    String read;
    String type;
    String from;
    String time_stamp;
    String title;
    boolean unread_attachment;
}
