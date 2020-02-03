package com.cyworld.social.utils.response.friend;

import lombok.Builder;
import lombok.Data;

@Data
public class res_one_invite {
    String from_id;
    String from_namespace;
    String type;
    long invite_time;

    public res_one_invite(String from_id,String from_namespace,String type,long invite_time){
        this.from_id=from_id;
        this.from_namespace=from_namespace;
        this.type=type;
        this.invite_time=invite_time;
    }
}
