package com.cyworld.social.utils.request.ranks;

import lombok.Builder;
import lombok.Data;

@Data
public class req_get_ranks {
    String type;
    int skip;
    int take;
}
