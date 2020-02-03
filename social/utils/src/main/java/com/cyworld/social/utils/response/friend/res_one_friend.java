package com.cyworld.social.utils.response.friend;

import lombok.Builder;
import lombok.Data;

@Data
public class res_one_friend {
    String _id;
    String namespace;
    int status;

    public res_one_friend(String id,String namespace,int status){
        this._id=id;
        this.namespace=namespace;
        this.status=status;
    }
}
