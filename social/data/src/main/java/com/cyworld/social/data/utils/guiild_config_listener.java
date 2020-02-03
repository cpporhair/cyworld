package com.cyworld.social.data.utils;

import com.alibaba.fastjson.JSON;
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

@Component
public class guiild_config_listener {
    private static final Logger logger = LoggerFactory.getLogger(Mail_config_listener.class.getName());

    @Value
    public static class config {
        private int max_vp_count;
        private int max_officer_count;
    }

    private static guiild_config_listener.config this_config = new guiild_config_listener.config(2, 3);
    public static AtomicReference<guiild_config_listener.config> ar = new AtomicReference(this_config);

    @Autowired
    private Environment environment;

    //@PostConstruct
    public void init() throws Exception {
        config_listener.start_listener(
                environment.getProperty("spring.cloud.nacos.config.server-addr"),
                "GUILD_CONFIG",
                "DATA_SERVICE",
                config_str -> {
                    JSONObject o = JSON.parseObject(config_str);
                    guiild_config_listener.config c = new guiild_config_listener.config
                            (
                                    o.containsKey("max_vp_count")?Integer.valueOf(o.getString("max_vp_count")):2,
                                    o.containsKey("max_officer_count")?Integer.valueOf(o.getString("max_officer_count")):3
                            );
                    ar.set(c);
                }
        );
    }
}
