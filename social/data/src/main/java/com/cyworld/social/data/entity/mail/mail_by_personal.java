package com.cyworld.social.data.entity.mail;

import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Builder
@Data
public class mail_by_personal implements Comparable<mail_by_personal> {

    @Builder.Default
    boolean is_read = false;

    @Builder.Default
    long time_stamp = System.currentTimeMillis();

    @Builder.Default
    List<mail_attachment> attachment = new ArrayList<>();

    String id;
    String id_box;
    String id_namespace;
    String type;
    String from;
    String title;
    String content;

    @Override
    public int compareTo(mail_by_personal mail_by_personal) {
        if (this.getTime_stamp() == mail_by_personal.getTime_stamp())
            return mail_by_personal.getId().compareTo(this.getId());
        return Long.valueOf(mail_by_personal.getTime_stamp()).compareTo(this.getTime_stamp());
    }

    public static mail_by_personal none = mail_by_personal.builder().id("none").build();

    public static mail_by_personal deep_clone(mail_by_personal o) {
        mail_by_personal res = mail_by_personal
                .builder()
                .is_read(o.is_read)
                .time_stamp(o.time_stamp)
                .id(o.id)
                .id_box(o.id_box)
                .id_namespace(o.id_namespace)
                .type(o.type)
                .attachment(new ArrayList<>())
                .from(o.from)
                .title(o.title)
                .content(o.content)
                .build();
        if (o.getAttachment() != null) {
            for (mail_attachment a : o.getAttachment()) {
                res.getAttachment().add(mail_attachment.deep_clone(a));
            }
        }
        return res;
    }
}
