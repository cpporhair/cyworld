package com.cyworld.social.utils.response.guild;

import lombok.Data;

import java.util.List;

@Data
public class res_get_guild_list {
    int max_count;
    List<res_guild_info_simple> list;
}
