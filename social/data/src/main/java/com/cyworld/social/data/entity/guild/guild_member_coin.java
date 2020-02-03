package com.cyworld.social.data.entity.guild;

import com.cyworld.social.data.utils.reset_by_timer_point;
import lombok.Data;

@Data
public class guild_member_coin implements reset_by_timer_point.target {
    public long coin=0;
    @Override
    public void do_reset() {
        coin=0;
    }

    @Override
    public reset_by_timer_point.target deep_clone() {
        guild_member_coin res=new guild_member_coin();
        res.coin=this.coin;
        return res;
    }
}
