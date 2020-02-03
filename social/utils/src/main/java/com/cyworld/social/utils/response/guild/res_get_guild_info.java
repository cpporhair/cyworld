package com.cyworld.social.utils.response.guild;

import com.alibaba.fastjson.JSONObject;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
public class res_get_guild_info {
    int empty_flag;
    String id;
    String name;
    String language;
    String flag;
    String owner_id;
    String owner_namespace;
    String owner_attachment_json;
    String log;
    int pos;
    long exp;
    int member_count;
    int max_member_count;
    int player_level_for_join;
    boolean need_apply;
    String declaration;
    String notice;
    long power;
}
