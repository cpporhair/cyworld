package com.cyworld.social.data.entity.guild;

import com.cyworld.social.data.entity.base.i_deep_clone;
import lombok.Builder;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;

@Data
@Builder
public class player_guild_data implements i_deep_clone<player_guild_data> {
    String id;
    String namespace;
    String guild_id;

    public boolean is_guild_empty(){
        if(guild_id==null)
            return true;
        if(guild_id.isEmpty())
            return true;
        if(guild_id.toUpperCase().equals("NULL"))
            return true;
        return false;
    }

    @Override
    public player_guild_data deep_clone() {
        return player_guild_data.builder()
                .id(this.id)
                .namespace(this.namespace)
                .guild_id(this.guild_id)
                .build();
    }
}
