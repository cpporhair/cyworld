package com.cyworld.social.utils.response.mail;

import lombok.Data;

import java.util.List;

@Data
public class res_delete_mail {
    List<res_get_mail_detail> mails;
}
