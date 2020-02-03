package com.cyworld.social.data.entity.mail;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class subscription {
    String mailbox_ID;
    String mailgroup_ID;
    long last_get_timer;

    public static subscription deep_clone(subscription o) {
        return subscription
                .builder()
                .last_get_timer(o.last_get_timer)
                .mailbox_ID(o.mailbox_ID)
                .mailgroup_ID(o.mailgroup_ID)
                .build();
    }
}
