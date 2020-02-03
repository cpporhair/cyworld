package com.cyworld.social.utils.request.global;

import lombok.Data;

@Data
public class req_new_user {
    String player_id;
    String namespace;
    String password;
    String nickname;
}
