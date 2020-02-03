package com.cyworld.social.gateway;

import lombok.Data;
import org.springframework.cloud.gateway.route.RouteDefinition;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

import javax.validation.Valid;
import javax.validation.constraints.NotEmpty;
import java.util.ArrayList;
import java.util.List;

@Data
public class ConfigRecv {
    String ver = "1";
    List<RouteDefinition> definition = new ArrayList<>();
}
