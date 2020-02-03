package com.cyworld.social.data.mongodb;

import com.cyworld.social.data.actors.akka_system;
import com.mongodb.reactivestreams.client.MongoClient;
import com.mongodb.reactivestreams.client.MongoClients;
import com.mongodb.reactivestreams.client.MongoDatabase;
import lombok.Builder;
import lombok.Data;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

import javax.annotation.PostConstruct;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Component
public class db_context {

    @Data
    @Builder
    public static class mongo_context {
        MongoClient mongo_client;
        ReactiveMongoTemplate template;
    }

    @Autowired
    private Environment environment;
    @Autowired
    akka_system akka;

    @PostConstruct
    private void init() {
        try {
            Flux.fromArray(environment.getProperty("mongo_centers").split(";"))
                    .map(one_center -> {
                        String s[] = one_center.split("@");
                        MongoClient c = MongoClients.create(s[1]);
                        return mongo_context
                                .builder()
                                .mongo_client(c)
                                .template(new ReactiveMongoTemplate(c, s[0]))
                                .build();
                    })
                    .doOnNext(r -> {
                        mongo_templates.put(r.template.getMongoDatabase().getName(), r);
                    })
                    .subscribe();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private db_context() {
    }

    public static ConcurrentMap<String, mongo_context> mongo_templates = new ConcurrentHashMap<String, mongo_context>();
}
