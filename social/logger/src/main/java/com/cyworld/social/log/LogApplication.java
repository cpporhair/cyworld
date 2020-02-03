package com.cyworld.social.log;


import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.net.server.ServerSocketReceiver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.Environment;
import reactor.netty.Connection;
import reactor.netty.tcp.TcpClient;
import reactor.netty.tcp.TcpServer;

import javax.annotation.PostConstruct;
import java.util.concurrent.Executors;

@EnableDiscoveryClient
@SpringBootApplication
public class LogApplication {
    private static final Logger logger = LoggerFactory.getLogger(LogApplication.class.getName());

    public static void main(String[] args) {
        SpringApplication.run(LogApplication.class, args);
    }

    @Autowired
    Environment environment;

    @PostConstruct
    public void init() {
        ch.qos.logback.classic.Logger root =
                (ch.qos.logback.classic.Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);
        //root.setLevel(Level.DEBUG);
        LoggerContext rootCtx = (LoggerContext) LoggerFactory.getILoggerFactory();
        ServerSocketReceiver recv = new ServerSocketReceiver();
        recv.setContext(rootCtx);
        recv.setPort(5801);
        recv.start();
        logger.info("stared");
    }

    //@Override
    public void run(String... args) throws Exception {
    }
}
