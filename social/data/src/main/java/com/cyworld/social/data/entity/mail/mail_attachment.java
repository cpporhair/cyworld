package com.cyworld.social.data.entity.mail;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class mail_attachment {

    @Builder.Default
    boolean read = false;

    String id;
    String content;

    public static mail_attachment deep_clone(mail_attachment o) {
        return mail_attachment
                .builder()
                .read(o.read)
                .id(o.id)
                .content(o.content)
                .build();
    }
}
