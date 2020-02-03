package com.cyworld.social.utils.response.mail;

import lombok.Data;

import java.util.List;

@Data
public class res_count_unread_personal_mail {
    List<mail_type_count> datas;
}
