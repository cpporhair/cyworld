package com.cyworld.social.data.service.mail;

import akka.actor.ActorRef;
import com.alibaba.fastjson.JSON;
import com.cyworld.social.data.actors.akka_system;
import com.cyworld.social.data.actors.mail.actor_mailbox;
import com.cyworld.social.data.actors.public_message_define;
import com.cyworld.social.data.entity.mail.mailbox;
import com.cyworld.social.utils.request.mail.req_read_all_mail;
import lombok.Data;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.List;
import java.util.stream.Collectors;

import static akka.pattern.Patterns.ask;
@Component
public class srv_read_all_mail {
    @Autowired
    akka_system akka;
    @Data
    public static class context {
        req_read_all_mail req;
        ActorRef actor_mailbox_ref;
    }

    public Mono serv(ServerRequest request) {
        context this_context = new context();

        return request.bodyToMono(req_read_all_mail.class)
                .flatMap(body -> {
                    this_context.req = body;
                    return actor_mailbox.find_or_build(
                            body.getNamesapce(),
                            body.getId(),
                            akka.actor_system
                    );
                })
                .doOnNext(this_context::setActor_mailbox_ref)
                .doOnNext(ref -> {
                    ref.tell(
                            (actor_mailbox.msg_inject_tell<mailbox>)data->{
                                data.getInbox().forEach(e->{
                                    if(e.getType().equals(this_context.req.getType()))
                                        e.set_read(true);
                                });
                            },
                            ActorRef.noSender()
                    );
                    ref.tell(public_message_define.SAVE_SIGNAL,ActorRef.noSender());
                })
                .flatMap(res -> {
                    return ServerResponse.ok()
                            .body(BodyInserters.fromObject("OK"));
                });
    }
}
