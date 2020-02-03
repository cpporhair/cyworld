package com.cyworld.social.data.entity.friends;

import lombok.Builder;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Data
@Builder
public class friend{
    String _id;
    String namespace;
    int status;
    @Builder.Default
    List<friend_message> messages=new ArrayList<>();

    public friend deep_clone() {
        return deep_clone(this);
    }


    public static friend deep_clone(friend f){
        friend new_f=friend
                .builder()
                ._id(f.get_id())
                .namespace(f.getNamespace())
                .status(f.getStatus())
                .build();
        new_f.messages=f.messages.stream().map(d->{return d.deep_clone();}).collect(Collectors.toList());
        return new_f;
    }
}
