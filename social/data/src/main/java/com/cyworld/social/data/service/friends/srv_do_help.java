package com.cyworld.social.data.service.friends;

import akka.actor.ActorRef;
import com.cyworld.social.data.actors.akka_system;
import com.cyworld.social.data.actors.friends.actor_friends_data;
import com.cyworld.social.data.actors.public_message_define;
import com.cyworld.social.data.entity.friends.help_data;
import com.cyworld.social.data.entity.friends.player_friends_data;
import com.cyworld.social.data.mongodb.db_context;
import com.cyworld.social.utils.request.friend.req_do_help;
import lombok.Data;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

import java.time.Duration;

import static akka.pattern.Patterns.ask;
@Component
public class srv_do_help {

    @Autowired
    akka_system akka;

    @Data
    private static class context{
        req_do_help req;
        ActorRef player_ref;
        ActorRef target_ref;
    }
    public Mono serv(ServerRequest request) {
        context this_context = new context();
        return request.bodyToMono(req_do_help.class)
                .doOnNext(req -> {
                    this_context.req = req;
                })
                .flatMap(req -> {
                    return actor_friends_data.find_or_build(
                            req.getPlayer_namespace(),
                            req.getPlayer_id(),
                            akka.actor_system
                    );
                })
                .doOnNext(ref -> {
                    this_context.player_ref = ref;
                })
                .flatMap(ref -> {
                    return actor_friends_data.find_or_build(
                            this_context.req.getTarget_namespace(),
                            this_context.req.getTarget_id(),
                            akka.actor_system
                    );
                })
                .doOnNext(ref -> {
                    this_context.target_ref = ref;
                })
                .flatMap(ref->{
                    if(this_context.target_ref.isTerminated())
                        return Mono.error(new Throwable("cant find target"));
                    if(this_context.player_ref.isTerminated())
                        return Mono.error(new Throwable("cant find player"));
                    return Mono.fromCompletionStage
                            (
                                    ask
                                            (
                                                    this_context.target_ref,
                                                    (actor_friends_data.msg_inject_ask<player_friends_data,Boolean>) data->{
                                                        data.update_current_help_data();
                                                        if (data.getCurrent_help_data()==null)
                                                            return Boolean.FALSE;
                                                        if (data.getCurrent_help_data()
                                                                .getDonors()
                                                                .stream()
                                                                .anyMatch(o->o.equals
                                                                        (
                                                                                this_context.req.getPlayer_namespace()
                                                                                        .concat(".")
                                                                                        .concat(this_context.req.getPlayer_id())
                                                                        )))
                                                            return Boolean.FALSE;

                                                        data.getCurrent_help_data()
                                                                .getDonors()
                                                                .add
                                                                        (
                                                                                this_context.req.getPlayer_namespace()
                                                                                        .concat(".")
                                                                                        .concat(this_context.req.getPlayer_id())
                                                                        );

                                                        data.getCurrent_help_data()
                                                                .setHelp_amount
                                                                        (
                                                                                data.getCurrent_help_data().getHelp_amount()>this_context.req.getAmount()
                                                                                        ?data.getCurrent_help_data().getHelp_amount()-this_context.req.getAmount()
                                                                                        :0
                                                                        );
                                                        return Boolean.TRUE;
                                                    },
                                                    Duration.ofSeconds(1)
                                            )
                                            .thenApply(o->{return Boolean.class.cast(o);})

                            );
                })
                .doOnNext(res->{
                    if(res){
                        this_context.target_ref.tell(
                                public_message_define.SAVE_SIGNAL,
                                ActorRef.noSender()
                        );
                    }
                })
                .flatMap(res->{
                    return ServerResponse.ok().body(BodyInserters.fromObject(this_context.req));
                });
    }
}
