package com.cyworld.social.data.entity.guild;

import com.cyworld.social.data.entity.base.i_deep_clone;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class guild_ask_info_player implements i_deep_clone<guild_ask_info_player> {
    String guild_id;
    Long expiration;
    int status;

    @Override
    public guild_ask_info_player deep_clone() {
        return guild_ask_info_player.builder()
                .guild_id(this.guild_id)
                .expiration(this.expiration)
                .status(this.status)
                .build();
    }
}
