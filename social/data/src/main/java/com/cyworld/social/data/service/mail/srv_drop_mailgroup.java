package com.cyworld.social.data.service.mail;

import akka.actor.ActorRef;
import com.cyworld.social.data.actors.akka_system;
import com.cyworld.social.data.actors.mail.actor_mailbox;
import com.cyworld.social.data.actors.mail.actor_mailgroup;
import com.cyworld.social.data.actors.public_message_define;
import com.cyworld.social.data.entity.mail.mailbox;
import com.cyworld.social.data.entity.mail.mailgroup;
import com.cyworld.social.utils.request.mail.req_drop_mailgroup;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;
@Component
public class srv_drop_mailgroup {
    public static class service_context {
        req_drop_mailgroup req;
        ActorRef box_ref;
    }

    @Autowired
    akka_system akka;

    public Mono serv(ServerRequest request) {
        service_context context = new service_context();
        return request.bodyToMono(req_drop_mailgroup.class)
                .flatMap(body -> {
                    context.req = body;
                    return actor_mailgroup.find_or_build(body.getMail_group_id(),akka.actor_system)
                            .doOnNext(ref->{
                                ref.tell(
                                        (actor_mailgroup.msg_inject_tell<mailgroup>)grp->{
                                            grp.setOpening(false);
                                        },
                                        ActorRef.noSender()
                                );
                                ref.tell(
                                        public_message_define.SAVE_NOW_SIGNAL,
                                        ActorRef.noSender()
                                );
                                ref.tell(
                                        public_message_define.CLOSE_SIGNAL,
                                        ActorRef.noSender()
                                );
                            });
                })
                .flatMap(unused -> {
                    return ServerResponse
                            .ok()
                            .body(BodyInserters.fromObject("OK"));
                });

    }
}
