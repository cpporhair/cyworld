package com.cyworld.social.data.utils;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.cyworld.social.utils.nacos.config_listener;
import lombok.Value;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.concurrent.atomic.AtomicReference;


public class rank_config_listener {
    private static final Logger logger = LoggerFactory.getLogger(Mail_config_listener.class.getName());

    @Autowired
    private Environment environment;

    @PostConstruct
    public void init() throws Exception {
        config_listener.start_listener(
                environment.getProperty("spring.cloud.nacos.config.server-addr"),
                "RANK_CONFIG",
                "DATA_SERVICE",
                config_str -> {
                    JSONArray a = JSON.parseArray(config_str);
                    for (Object o : a) {
                        JSONObject j=JSONObject.class.cast(o);
                        String type=j.getString("type");
                        String max_count=j.getString("max_count");
                    }
                }
        );
    }
}
