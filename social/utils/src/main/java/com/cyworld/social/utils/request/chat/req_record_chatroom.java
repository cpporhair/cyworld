package com.cyworld.social.utils.request.chat;

import lombok.Data;

@Data
public class req_record_chatroom {
    String id;
    String name;
    String owner;
    String affiliations_count;
}
