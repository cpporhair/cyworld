package com.cyworld.social.global;

import com.cyworld.social.utils.log.util_logger_factory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

import javax.annotation.PostConstruct;
import java.util.List;

@SpringBootApplication
@EnableDiscoveryClient
@Configuration
public class GlobalApplication {
    static util_logger_factory util_logger_factory = new util_logger_factory();
    private static final Logger logger = LoggerFactory.getLogger(GlobalApplication.class.getName());

    @Autowired
    Environment environment;

    @PostConstruct
    private void init() throws Exception {
        util_logger_factory.init(environment);
    }

    public static void main(String[] args) {
        SpringApplication.run(GlobalApplication.class, args);
    }

}
