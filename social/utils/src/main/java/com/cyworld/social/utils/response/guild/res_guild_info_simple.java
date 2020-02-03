package com.cyworld.social.utils.response.guild;

import com.alibaba.fastjson.JSONObject;
import lombok.Data;

@Data
public class res_guild_info_simple{
    String id;
    String name;
    String flag;
    String owner_id;
    String owner_namespace;
    int level_for_join;
    boolean need_apply;
    boolean asked;
    long power;
    long exp;
    int member_count;
    int max_count;
    String owner_attachment_json;
}
