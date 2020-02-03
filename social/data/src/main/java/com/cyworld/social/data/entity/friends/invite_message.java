package com.cyworld.social.data.entity.friends;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class invite_message{
    String from_id;
    String from_namespace;
    String type;
    long invite_time;

    public invite_message deep_clone() {
        return invite_message.deep_clone(this);
    }
    public static invite_message deep_clone(invite_message o){
        return o.builder()
                .from_id(o.getFrom_id())
                .from_namespace(o.getFrom_namespace())
                .type(o.type)
                .invite_time(o.invite_time)
                .build();
    }
}
