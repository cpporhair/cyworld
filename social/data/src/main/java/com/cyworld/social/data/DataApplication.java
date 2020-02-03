package com.cyworld.social.data;


import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

@SpringBootApplication
@EnableDiscoveryClient
@Configuration
public class DataApplication {
    private static final Logger logger = LoggerFactory.getLogger(DataApplication.class.getName());

    static public void main(String... args) {
        SpringApplication.run(DataApplication.class, args);
    }
}
