package com.cyworld.social.utils.response.guild;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
public class res_get_ask_list_component {
    String player_id;
    String player_namespace;
    long timer;
    long power;
    String json_game_data;
}
