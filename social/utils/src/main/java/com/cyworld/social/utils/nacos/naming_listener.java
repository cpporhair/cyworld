package com.cyworld.social.utils.nacos;

import com.alibaba.nacos.api.NacosFactory;
import com.alibaba.nacos.api.config.ConfigService;
import com.alibaba.nacos.api.config.listener.Listener;
import com.alibaba.nacos.api.naming.NamingService;
import com.alibaba.nacos.api.naming.listener.NamingEvent;
import com.alibaba.nacos.api.naming.pojo.Instance;

import java.util.List;
import java.util.concurrent.Executor;
import java.util.function.Consumer;

public class naming_listener {
    public static void start_listener(
            String nacos_config_server_addr,
            String service_name,
            Consumer<NamingEvent> cb) throws Exception {
        NamingService service = NacosFactory.createNamingService(nacos_config_server_addr);
        service.subscribe(service_name, e -> {
            if (e instanceof NamingEvent) {
                cb.accept(NamingEvent.class.cast(e));
            }
        });
    }

    public static List<Instance> get_naming(String nacos_config_server_addr,
                                            String service_name) throws Exception {
        NamingService service = NacosFactory.createNamingService(nacos_config_server_addr);
        return service.getAllInstances(service_name);
    }
}
