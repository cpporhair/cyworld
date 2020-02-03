package com.cyworld.social.friends;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.context.annotation.Configuration;

@SpringBootApplication
@EnableDiscoveryClient
@Configuration
public class FriendsApplication {
    private static final Logger logger = LoggerFactory.getLogger(FriendsApplication.class.getName());
    public static void main(String[] args) {
        SpringApplication.run(FriendsApplication.class, args);
    }

}
