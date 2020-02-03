package com.cyworld.social.data.entity.guild;

import com.cyworld.social.data.utils.reset_by_timer_point;
import lombok.Data;

@Data
public class guild_member_donate_experience implements reset_by_timer_point.target {
    public Long exp=Long.valueOf(0);
    @Override
    public void do_reset() {
        exp=Long.valueOf(0);
    }

    @Override
    public reset_by_timer_point.target deep_clone() {
        guild_member_donate_experience res=new guild_member_donate_experience();
        res.exp=this.exp.longValue();
        return res;
    }
}
