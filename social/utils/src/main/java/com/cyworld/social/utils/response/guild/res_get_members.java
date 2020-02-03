package com.cyworld.social.utils.response.guild;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class res_get_members {
    String id;
    List<res_get_members_component> component=new ArrayList<>();
}
