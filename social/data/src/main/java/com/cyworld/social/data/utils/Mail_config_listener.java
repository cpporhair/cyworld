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
public class Mail_config_listener {
    private static final Logger logger = LoggerFactory.getLogger(Mail_config_listener.class.getName());

    @Value
    public static class config {
        private int max_mail_personal;
        private int max_mail_broadcast;
        private long mail_personal_keep_millis;
        private long mail_broadcast_keep_millis;
        private String[] delete_order;
    }

    private static final long default_30_day=30*24*3600*1000L;

    private static config this_config = new config(30, 30,default_30_day, default_30_day, new String[]{"1", "3", "2"});
    public static AtomicReference<config> ar = new AtomicReference(this_config);

    @Autowired
    private Environment environment;

    @PostConstruct
    public void init() throws Exception {
        config_listener.start_listener(
                environment.getProperty("spring.cloud.nacos.config.server-addr"),
                "MAIL_CONFIG",
                "DATA_SERVICE",
                config_str -> {
                    JSONObject o = JSON.parseObject(config_str);
                    config c = new config
                            (
                                    o.containsKey("max_mail_personal")?Integer.valueOf(o.getString("max_mail_personal")):30,
                                    o.containsKey("max_mail_broadcast")?Integer.valueOf(o.getString("max_mail_broadcast")):30,
                                    o.containsKey("mail_personal_keep_millis")?Long.valueOf(o.getString("mail_personal_keep_millis")):default_30_day,
                                    o.containsKey("mail_broadcast_keep_millis")?Long.valueOf(o.getString("mail_broadcast_keep_millis")):default_30_day,
                                    o.containsKey("delete_order")?o.getString("delete_order").split(";"):new String[]{"1", "3", "2"}
                            );
                    ar.set(c);
                }
        );
    }
}
