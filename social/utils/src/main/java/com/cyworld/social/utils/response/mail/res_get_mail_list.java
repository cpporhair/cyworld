package com.cyworld.social.utils.response.mail;

import lombok.Data;

import java.util.List;

@Data
public class res_get_mail_list {
    Long max_size;
    Long mail_keep_timer_in_config;
    List<mail_list_info> mails;
}
