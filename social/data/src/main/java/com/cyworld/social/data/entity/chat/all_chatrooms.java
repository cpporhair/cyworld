package com.cyworld.social.data.entity.chat;

import com.cyworld.social.data.entity.base.i_deep_clone;
import lombok.Data;

import java.util.HashMap;
import java.util.Map;

@Data
public class all_chatrooms implements i_deep_clone<all_chatrooms> {
    Map<String,chatroom> data=new HashMap<>();
    public all_chatrooms deep_clone() {
        return null;
    }
}
