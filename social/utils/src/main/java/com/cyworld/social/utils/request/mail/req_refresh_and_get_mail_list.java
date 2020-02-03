package com.cyworld.social.utils.request.mail;

import lombok.Data;

@Data
public class req_refresh_and_get_mail_list {
    String id;
    String namesapce;
    String type;
    String skip;
    String take;
    String value_Json;

    public req_refresh_mailbox_inbox get_req_refresh_mailbox_inbox() {
        req_refresh_mailbox_inbox req = new req_refresh_mailbox_inbox();
        req.setId(this.getId());
        req.setNamespace(this.getNamesapce());
        req.setValue_Json(this.getValue_Json());
        return req;
    }

    public req_get_mail_list get_req_get_mail_list() {
        req_get_mail_list req = new req_get_mail_list();
        req.setId(this.getId());
        req.setNamesapce(this.getNamesapce());
        req.setType(this.type);
        req.setSkip(this.getSkip());
        req.setTake(this.getTake());
        return req;
    }
}
