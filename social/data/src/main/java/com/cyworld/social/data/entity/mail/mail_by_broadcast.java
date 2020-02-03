package com.cyworld.social.data.entity.mail;

import lombok.Builder;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
@Builder
public class mail_by_broadcast implements Comparable<mail_by_broadcast> {
    @Builder.Default
    boolean is_read = false;

    @Builder.Default
    long time_stamp = System.currentTimeMillis();

    @Builder.Default
    List<mail_attachment> attachment = new ArrayList<>();

    String id;
    String id_group;
    String type;
    String from;
    String title;
    String content;
    String condition;

    public static mail_by_broadcast deep_clone(mail_by_broadcast o) {
        mail_by_broadcast res = mail_by_broadcast
                .builder()
                .is_read(o.is_read)
                .time_stamp(o.time_stamp)
                .id(o.id)
                .id_group(o.id_group)
                .type(o.type)
                .from(o.from)
                .title(o.title)
                .content(o.content)
                .condition(o.condition)
                .build();
        if (o.getAttachment() != null) {
            for (mail_attachment a : o.getAttachment()) {
                res.getAttachment().add(mail_attachment.deep_clone(a));
            }
        }
        return res;
    }

    @Override
    public int compareTo(mail_by_broadcast mail_by_broadcast) {
        if (this.getTime_stamp() == mail_by_broadcast.getTime_stamp())
            return mail_by_broadcast.getId().compareTo(this.getId());
        return Long.valueOf(mail_by_broadcast.getTime_stamp()).compareTo(this.getTime_stamp());
    }
}
