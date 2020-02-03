package com.cyworld.social.data.entity.guild;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.cyworld.social.data.utils.reset_by_timer_point;
import lombok.Data;

@Data
public class guild_member_donate_log implements reset_by_timer_point.target {
    public JSONObject donate=new JSONObject();
    @Override
    public void do_reset() {
        donate=new JSONObject();
    }

    @Override
    public reset_by_timer_point.target deep_clone() {
        guild_member_donate_log res=new guild_member_donate_log();
        res.donate= JSON.parseObject(this.donate.toJSONString());
        return res;
    }
}
