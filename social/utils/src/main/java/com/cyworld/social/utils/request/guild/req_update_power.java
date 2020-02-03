package com.cyworld.social.utils.request.guild;

import lombok.Data;

import java.util.List;

@Data
public class req_update_power {
    List<req_update_power_component> component;
}
