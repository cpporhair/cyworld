package com.cyworld.social.data.entity.friends;

import com.cyworld.social.data.entity.base.i_deep_clone;
import lombok.Builder;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Data
@Builder
public class player_friends_data implements i_deep_clone<player_friends_data> {
    String _id;
    String namespace;
    int status;
    @Builder.Default
    List<friend> friends=new ArrayList<>();
    @Builder.Default
    List<friend> blacklist=new ArrayList<>();
    @Builder.Default
    List<invite_message> all_invite=new ArrayList<>();
    help_data current_help_data;

    public void update_current_help_data(){
        if(current_help_data==null)
            return;
        if(System.currentTimeMillis()>current_help_data.getTimeout())
            current_help_data=null;
    }

    public invite_message remove_invite_message(String namespace,String id){
        for(int i=0;i<all_invite.size();++i){
            invite_message o=all_invite.get(i);
            if(o.getFrom_id().equals(id) && o.getFrom_namespace().equals(namespace)){
                all_invite.remove(i);
                return o;
            }
        }
        return null;
    }
    public boolean add_friend(friend fri){
        if(friends.stream()
                .anyMatch(old->{return old._id.equals(fri._id)&&old.namespace.equals(fri.namespace);}))
            return false;
        friends.add(fri);
        return true;
    }
    public friend remove_friend(String namespace,String id){
        for(int i=0;i<friends.size();++i){
            friend f=friends.get(i);
            if(f.namespace.equals(namespace)&&f._id.equals(id)){
                friends.remove(i);
                return f;
            }
        }
        return null;
    }
    public friend remove_blacklist(String namespace,String id){
        for(int i=0;i<blacklist.size();++i){
            friend f=blacklist.get(i);
            if(f.namespace.equals(namespace)&&f._id.equals(id)){
                blacklist.remove(i);
                return f;
            }
        }
        return null;
    }
    public boolean add_blacklist(friend fri){
        if(blacklist.stream()
                .anyMatch(old->{return old._id.equals(fri._id)&&old.namespace.equals(fri.namespace);}))
            return false;
        blacklist.add(fri);
        return true;
    }
    public void set_friend_to_blacklist(String namespace,String id){
        friend f=remove_friend(namespace,id);
        if(f!=null)
            add_blacklist(f);
    }
    public void set_friend_top(String namespace,String id){
        friends.stream().forEach(o->{
            if(o.get_id().equals(id)&&o.getNamespace().equals(namespace))
                o.setStatus(1);
            else
                o.setStatus(0);
        });
    }
    public static player_friends_data deep_clone(player_friends_data data){
        player_friends_data new_data=player_friends_data
                .builder()
                .namespace(data.namespace)
                ._id(data._id)
                .status(data.status)
                .current_help_data(data.current_help_data==null?null:data.current_help_data.deep_clone())
                .build();
        new_data.friends=data.friends.stream().map(o->{return o.deep_clone();}).collect(Collectors.toList());
        new_data.all_invite=data.all_invite.stream().map(o->{return o.deep_clone();}).collect(Collectors.toList());
        new_data.blacklist=data.blacklist.stream().map(o->{return o.deep_clone();}).collect(Collectors.toList());
        return new_data;
    }
    public player_friends_data deep_clone(){
        return player_friends_data.deep_clone(this);
    }
}
