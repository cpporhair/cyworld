package com.cyworld.social.data.service.mail;

import akka.actor.ActorRef;
import com.cyworld.social.data.actors.common.actor_entity;
import com.cyworld.social.data.actors.common.actor_transaction;
import com.cyworld.social.data.actors.mail.actor_mailbox;
import com.cyworld.social.data.actors.mail.actor_mailgroup;
import com.cyworld.social.data.entity.mail.mailbox;
import com.cyworld.social.data.service.transaction.srv_helper;
import com.cyworld.social.utils.request.mail.req_new_subscribe;
import com.cyworld.social.data.actors.*;
import com.cyworld.social.data.entity.mail.subscription;
import com.cyworld.social.data.utils.static_define;

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
public class srv_subscribe {

    public static class service_context {
        req_new_subscribe req;
        String tx_token;
        ActorRef box_ref;
        ActorRef txc_ref;
    }

    @Autowired
    akka_system akka;

    public Mono serv(ServerRequest request) {
        service_context context = new service_context();
        context.tx_token = srv_helper.get_transaction_token(request);
        boolean in_transaction = !context.tx_token.equals(static_define.no_in_transaction);
        return request.bodyToMono(req_new_subscribe.class)
                .flatMap(body -> {
                    context.req = body;
                    return Flux.fromArray(body.getGroups().split(";"))
                            .flatMap(s -> {
                                return actor_mailgroup.find_or_build(s, akka.actor_system)
                                        .flatMap(ref -> {
                                            return ref.isTerminated()
                                                    ? Mono.error(new Throwable("cant find mailgroup".concat(s)))
                                                    : Mono.just(1);
                                        });
                            })
                            .then(actor_mailbox.find_or_build(body.getServer(), body.getPlayer(), akka.actor_system));
                })
                .doOnNext(ref -> {
                    context.box_ref = ref;
                })
                .flatMap(ref -> {
                    if (ref.isTerminated())
                        return Mono.error(new Throwable("cant find mailbox".concat(context.req.getPlayer())));
                    if (!in_transaction)
                        return Mono.just(ref);
                    return actor_entity.join_transaction(ref,context.tx_token);
                })
                .doOnNext(ref -> context.txc_ref = ref)
                .flatMap(ref -> {
                    return Mono.fromCompletionStage
                            (
                                    ask
                                            (
                                                    ref,
                                                    ((actor_mailbox.msg_inject_ask<mailbox,Integer>) box -> {
                                                        String grps[] = context.req.getGroups().split(";");
                                                        int i = 0;
                                                        for (String grp : grps) {
                                                            if (box.getMail_groups().stream().anyMatch(g -> g.getMailgroup_ID().equals(grp)))
                                                                continue;
                                                            subscription v = subscription
                                                                    .builder()
                                                                    .mailgroup_ID(grp)
                                                                    .mailbox_ID(box.get_id())
                                                                    .last_get_timer(System.currentTimeMillis())
                                                                    .build();
                                                            box.getMail_groups().add(v);
                                                            i++;
                                                        }
                                                        return i;
                                                    }),
                                                    Duration.ofSeconds(1)
                                            )
                            );
                })
                .flatMap(unused -> {
                    return ServerResponse
                            .ok()
                            .body(BodyInserters.fromObject("OK"));
                })
                .doOnError(unused -> {
                    if (context.txc_ref != null) {
                        context.txc_ref.tell
                                (
                                        ((actor_transaction.msg_undo) () -> {
                                            return context.tx_token;
                                        }),
                                        ActorRef.noSender()
                                );
                    }
                })
                .doOnSuccess(unused -> {
                    if (!in_transaction) {
                        context.box_ref.tell(public_message_define.SAVE_SIGNAL,ActorRef.noSender());
                    } else {
                        actor_transaction.simple_msg_push_tx_context_teller(
                                context.txc_ref.path().toString(),
                                context.tx_token,
                                akka.actor_system
                        );
                    }

                });

    }
}
