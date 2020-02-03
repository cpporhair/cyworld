package com.cyworld.social.utils.response.guild;

import lombok.Data;

import java.util.List;

@Data
public class res_get_guild_help_list {
    List<res_get_guild_help_component> myself;
    List<res_get_guild_help_component> others;
}
