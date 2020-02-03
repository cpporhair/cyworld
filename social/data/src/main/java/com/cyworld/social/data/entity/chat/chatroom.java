package com.cyworld.social.data.entity.chat;

import com.cyworld.social.data.entity.base.i_deep_clone;
import lombok.Builder;
import lombok.Data;

@Builder
@Data
public class chatroom implements i_deep_clone<chatroom> {
    String id;
    String name;
    String owner;
    String affiliations_count;

    @Override
    public chatroom deep_clone() {
        return null;
    }

}
