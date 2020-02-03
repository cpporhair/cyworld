package com.cyworld.social.utils.log;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.net.SocketAppender;
import ch.qos.logback.classic.net.server.ServerSocketAppender;
import com.alibaba.nacos.api.naming.pojo.Instance;
import com.cyworld.social.utils.nacos.naming_listener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

public class util_logger_factory {
    public void init(Environment environment) throws Exception {
        naming_listener.start_listener(
                environment.getProperty("spring.cloud.nacos.discovery.server-addr"),
                "LoggerService",
                e->{
                    if(e.getInstances().size()>0)
                        init(e.getInstances().get(0).getIp(), Integer.valueOf(e.getInstances().get(0).getMetadata().get("RecvPort")));
                }

        );
                /*
        Instance instance = naming_listener
                .get_naming(environment.getProperty("spring.cloud.nacos.discovery.server-addr"), "LoggerService")
                .get(0);
        init(instance.getIp(), Integer.valueOf(instance.getMetadata().get("RecvPort")));
        */
    }

    public void init(String logger_server_host, int port) {
        SocketAppender appender = new SocketAppender();
        appender.setName("RemoteLogger");
        appender.setRemoteHost(logger_server_host);
        appender.setPort(port);
        appender.setIncludeCallerData(true);
        appender.setContext((LoggerContext) LoggerFactory.getILoggerFactory());
        appender.start();
        ((ch.qos.logback.classic.Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME))
                .detachAppender("RemoteLogger");
        ((ch.qos.logback.classic.Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME))
                .addAppender(appender);
    }
}
