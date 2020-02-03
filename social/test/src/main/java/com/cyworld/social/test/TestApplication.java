package com.cyworld.social.test;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.event.ApplicationEnvironmentPreparedEvent;
import org.springframework.boot.origin.OriginTrackedValue;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.core.env.PropertySource;

import java.util.Map;

@SpringBootApplication
@EnableDiscoveryClient
@Configuration
public class TestApplication {

    public static class LocalEnvironmentPrepareEventListener implements ApplicationListener<ApplicationEnvironmentPreparedEvent> {

        @Override
        public void onApplicationEvent(ApplicationEnvironmentPreparedEvent event) {
            MutablePropertySources propertySources = event.getEnvironment().getPropertySources();
            for (PropertySource<?> propertySource : propertySources) {
                boolean applicationConfig = propertySource.getName().contains("applicationConfig");
                if (!applicationConfig) {
                    continue;
                }

                Map<String, OriginTrackedValue> source = (Map<String, OriginTrackedValue>) propertySource.getSource();
                OriginTrackedValue originTrackedValue = source.get("server.port");
                OriginTrackedValue newOriginTrackedValue = OriginTrackedValue.of("9999", originTrackedValue.getOrigin());
                source.put("server.port", newOriginTrackedValue);

                Object property = propertySource.getProperty("server.port");
                System.out.println(property);
            }
        }
    }

    public static void main(String[] args) {
        //SpringApplication app = new SpringApplication(TestApplication.class);
        //app.addListeners(new TestApplication.LocalEnvironmentPrepareEventListener());
        //app.run(args);
        SpringApplication.run(TestApplication.class, args);
    }

}
