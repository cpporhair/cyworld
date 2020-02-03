package com.cyworld.social.gateway;


import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.parser.Feature;
import com.cyworld.social.utils.nacos.config_listener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.gateway.route.RouteDefinition;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;
import org.yaml.snakeyaml.Yaml;

import javax.annotation.PostConstruct;

@Component
public class dynamic_route_serviceImplByNacos {
    @Autowired
    private dynamic_route_serviceImpl dynamicRouteService;
    @Autowired
    private Environment environment;

    public dynamic_route_serviceImplByNacos() {
        //dynamicRouteByNacosListener("GATEWAY_ROUTE_DEFINE","GATEWAY");
    }

    @PostConstruct
    public void dynamicRouteByNacosListener() throws Exception {
        config_listener.start_listener(
                environment.getProperty("spring.cloud.nacos.config.server-addr"),
                "GATEWAY_ROUTE_DEFINE",
                "GATEWAY",
                config_str -> {
                    Yaml yaml = new Yaml();
                    yaml.load(config_str);
                    ConfigRecv config = yaml.loadAs(config_str, ConfigRecv.class);
                    config.definition.forEach(d -> {
                        dynamicRouteService.update(d);
                    });
                }
        );
    }

}
