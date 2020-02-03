package com.cyworld.social.data.entity.mail;

import com.cyworld.social.data.entity.base.i_deep_clone;
import com.cyworld.social.data.utils.Mail_config_listener;
import lombok.Builder;
import lombok.Data;

import java.util.TreeSet;

@Data
@Builder
public class mailgroup implements i_deep_clone<mailgroup> {
    String id;
    String description;
    @Builder.Default
    boolean opening=true;
    TreeSet<mail_by_broadcast> inbox;

    public static mailgroup deep_clone(mailgroup o) {
        mailgroup g = mailgroup
                .builder()
                .id(o.id)
                .description(o.description)
                .opening(o.opening)
                .inbox(new TreeSet<>())
                .build();
        if (o.inbox != null) {
            for (mail_by_broadcast mail : o.inbox) {
                g.inbox.add(mail_by_broadcast.deep_clone(mail));
            }
        }
        return g;
    }

    @Override
    public mailgroup deep_clone() {
        return mailgroup.deep_clone(this);
    }

    public void clear_timeout(long timer){
        inbox.removeIf(e->{
            return e.time_stamp<timer;
        });
    }

    public void limit_inbox(int max,String type){
        if(inbox.size()<=max)
            return;
        inbox.removeIf(e->{
            if(inbox.size()<=max)
                return false;
            return (e.getType().equals(type));
        });
    }
    public void limit_inbox(int max,String[] order){
        for (int i = 0; i < order.length; i++) {
            limit_inbox(max,order[i]);
        }
    }

    public void clear_old(Mail_config_listener.config config){
        clear_timeout(System.currentTimeMillis()-config.getMail_broadcast_keep_millis());
        limit_inbox(config.getMax_mail_broadcast(),config.getDelete_order());
    }

}
