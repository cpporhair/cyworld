package com.cyworld.social.mail;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.context.annotation.Configuration;

@SpringBootApplication
@EnableDiscoveryClient
@Configuration
public class MailApplication {
    private static final Logger logger = LoggerFactory.getLogger(MailApplication.class.getName());

    public static void main(String[] args) {
        SpringApplication.run(MailApplication.class, args);
    }

}
