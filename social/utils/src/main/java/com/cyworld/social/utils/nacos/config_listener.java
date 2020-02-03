package com.cyworld.social.utils.nacos;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.nacos.api.NacosFactory;
import com.alibaba.nacos.api.config.ConfigService;
import com.alibaba.nacos.api.config.listener.Listener;

import java.util.concurrent.Executor;
import java.util.function.Consumer;

public class config_listener {
    public static void start_listener(
            String nacos_config_server_addr,
            String data_ID,
            String group,
            Consumer<String> cb) throws Exception {
        ConfigService configService = NacosFactory.createConfigService(nacos_config_server_addr);
        configService.addListener(data_ID, group, new Listener() {
            @Override
            public void receiveConfigInfo(String config_string) {
                cb.accept(config_string);
            }

            @Override
            public Executor getExecutor() {
                return null;
            }
        });
    }
}
