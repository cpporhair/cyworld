package com.cyworld.social.data.actors.guild;

import akka.actor.ActorRef;
import com.cyworld.social.data.actors.akka_system;
import com.cyworld.social.data.entity.guild.guild;
import com.cyworld.social.data.mongodb.db_context;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import reactor.core.publisher.Mono;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.time.Duration;

@Configuration
public class guild_actor_starter {
    @Autowired
    akka_system akka;


    @PostConstruct
    public void start_all(){
        Mono.just(1)
                .delayElement(Duration.ofMillis(100))
                .flatMap(unused->{
                    db_context.mongo_context c=db_context.mongo_templates.get("guild");
                    if(c==null)
                        return Mono.error(new Throwable());
                    else
                        return Mono.just(c);
                })
                .retry()
                .flatMap(c->{
                    return db_context.mongo_templates.get("guild").getTemplate()
                            .findAll(
                                    guild.class,
                                    "guilds"
                            )
                            .flatMap(g->{
                                return actor_guild.create(g,akka.actor_system,false);
                            })
                            .count();
                })
                .flatMap(n->{
                    return actor_cached_guild_list.find_or_build(akka.actor_system);
                })
                .doOnNext(ref->{
                    ref.tell("update", ActorRef.noSender());
                })
                .subscribe();

    }
}
