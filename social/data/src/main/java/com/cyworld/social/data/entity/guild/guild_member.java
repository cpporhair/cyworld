package com.cyworld.social.data.entity.guild;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.cyworld.social.data.entity.base.i_deep_clone;
import com.cyworld.social.data.utils.reset_by_timer_point;
import lombok.Builder;
import lombok.Data;

@Builder
@Data
public class guild_member implements i_deep_clone<guild_member> {
    String player_id;
    String player_namespace;
    String guild_id;
    long reg_timer;
    long power;

    @Builder.Default
    reset_by_timer_point.controller<guild_member_coin> coin
            =new reset_by_timer_point.controller<guild_member_coin>(new guild_member_coin());

    @Builder.Default
    reset_by_timer_point.controller<guild_member_donate_experience> donate_experience
            =new reset_by_timer_point.controller<guild_member_donate_experience>(new guild_member_donate_experience());

    @Builder.Default
    reset_by_timer_point.controller<guild_member_donate_log> donate_log
            =new reset_by_timer_point.controller<guild_member_donate_log>(new guild_member_donate_log());

    @Builder.Default
    int position=0;

    @Override
    public guild_member deep_clone() {
        return guild_member.builder()
                .player_id(this.player_id)
                .player_namespace(this.player_namespace)
                .guild_id(this.guild_id)
                .reg_timer(this.reg_timer)
                .power(this.getPower())
                .position(this.position)
                .coin(this.coin.deep_clone())
                .donate_experience(this.donate_experience.deep_clone())
                .donate_log(this.donate_log.deep_clone())
                .build();
    }
}
