package com.cyworld.social.data.entity.guild;

import com.cyworld.social.data.entity.base.i_deep_clone;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@AllArgsConstructor
public class guild_ask_info_guild implements i_deep_clone<guild_ask_info_guild> {
    String src_namespace;
    String src_id;
    long expiration;
    long power;

    @Override
    public guild_ask_info_guild deep_clone() {
        return guild_ask_info_guild.builder()
                .src_namespace(this.src_namespace)
                .src_id(this.src_id)
                .expiration(this.expiration)
                .power(this.power)
                .build();
    }
}
