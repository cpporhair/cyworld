package com.cyworld.social.data.entity.mail;

import com.cyworld.social.data.entity.base.i_deep_clone;
import com.cyworld.social.data.utils.Mail_config_listener;
import lombok.Builder;
import lombok.Data;
import org.springframework.data.annotation.Id;

import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;

@Data
@Builder
public class mailbox implements i_deep_clone<mailbox> {
    @Id
    String _id;
    String namespace;
    @Builder.Default
    TreeSet<mail_by_personal> inbox=new TreeSet<>();
    @Builder.Default
    List<subscription> mail_groups=new ArrayList<>();

    public static mailbox deep_clone(mailbox o) {
        mailbox res = mailbox
                .builder()
                ._id(o._id)
                .namespace(o.namespace)
                .inbox(new TreeSet<>())
                .mail_groups(new ArrayList<>())
                .build();
        if (o.getInbox() != null) {
            for (mail_by_personal m : o.getInbox()) {
                res.getInbox().add(mail_by_personal.deep_clone(m));
            }
        }
        if (o.getMail_groups() != null) {
            for (subscription s : o.getMail_groups()) {
                res.getMail_groups().add(subscription.deep_clone(s));
            }
        }
        return res;
    }

    @Override
    public mailbox deep_clone() {
        return mailbox.deep_clone(this);
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
        return;
        /*
        clear_timeout(System.currentTimeMillis()-config.getMail_personal_keep_millis());
        limit_inbox(config.getMax_mail_personal(),config.getDelete_order());
        */
    }
}
