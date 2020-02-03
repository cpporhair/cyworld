package com.cyworld.social.data.entity.friends;

import lombok.Builder;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Data
@Builder
public class help_data{
    String help_type;
    int help_amount;
    long timeout;
    @Builder.Default
    List<String> donors=new ArrayList<>();

    public help_data deep_clone() {
        return help_data.builder()
                .help_type(this.help_type)
                .help_amount(this.help_amount)
                .timeout(this.timeout)
                .donors(this.donors.stream().collect(Collectors.toList()))
                .build();
    }
}
