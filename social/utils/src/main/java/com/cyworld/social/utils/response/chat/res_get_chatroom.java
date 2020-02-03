package com.cyworld.social.utils.response.chat;

import lombok.Data;

@Data
public class res_get_chatroom {
    String id;
    String name;
    String owner;
    String affiliations_count;
}
