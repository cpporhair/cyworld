package com.cyworld.social.data.service.mail;

import akka.actor.ActorRef;
import com.cyworld.social.data.actors.akka_system;
import com.cyworld.social.data.actors.common.actor_entity;
import com.cyworld.social.data.actors.common.actor_transaction;
import com.cyworld.social.data.actors.mail.actor_mailbox;
import com.cyworld.social.data.actors.mail.actor_mailgroup;
import com.cyworld.social.data.actors.public_message_define;
import com.cyworld.social.data.entity.mail.mailbox;
import com.cyworld.social.data.entity.mail.subscription;
import com.cyworld.social.data.service.transaction.srv_helper;
import com.cyworld.social.data.utils.static_define;
import com.cyworld.social.utils.request.mail.req_new_subscribe;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;

import static akka.pattern.Patterns.ask;
@Component
public class srv_unsubscribe {

    public static class service_context {
        req_new_subscribe req;
        ActorRef box_ref;
    }

    @Autowired
    akka_system akka;

    public Mono serv(ServerRequest request) {
        service_context context = new service_context();
        return request.bodyToMono(req_new_subscribe.class)
                .flatMap(body -> {
                    context.req = body;
                    return actor_mailbox.find_or_build(body.getServer(), body.getPlayer(), akka.actor_system);
                })
                .doOnNext(ref -> {
                    context.box_ref = ref;
                })
                .flatMap(ref -> {
                    if (ref.isTerminated())
                        return Mono.error(new Throwable("cant find mailbox".concat(context.req.getPlayer())));
                    return Mono.just(ref);
                })
                .doOnNext(ref -> {
                    ref.tell(
                            ((actor_mailbox.msg_inject_tell<mailbox>)box->{
                                String grps[] = context.req.getGroups().split(";");
                                int i = 0;
                                box.getMail_groups().removeIf(e->{
                                    for (String grp:grps) {
                                        if(e.getMailgroup_ID().equals(grp))
                                            return true;
                                    }
                                    return false;
                                });
                            }),
                            ActorRef.noSender()
                    );
                    ref.tell(public_message_define.SAVE_SIGNAL,ActorRef.noSender());
                })
                .flatMap(unused -> {
                    return ServerResponse
                            .ok()
                            .body(BodyInserters.fromObject("OK"));
                });

    }
}
