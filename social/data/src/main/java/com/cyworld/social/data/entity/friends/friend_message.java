package com.cyworld.social.data.entity.friends;

import lombok.Builder;
import lombok.Data;

@Builder
@Data
public class friend_message{
    String msg;

    public friend_message deep_clone() {
        return deep_clone(this);
    }
    public static friend_message deep_clone(friend_message o){
        return friend_message
                .builder()
                .msg(o.msg)
                .build();
    }
}
