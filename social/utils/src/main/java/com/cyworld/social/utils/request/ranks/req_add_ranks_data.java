package com.cyworld.social.utils.request.ranks;

import lombok.Data;

@Data
public class req_add_ranks_data {
    String type;
    String ID;
    long compare_value;
    int max_count;
    String data;
}
