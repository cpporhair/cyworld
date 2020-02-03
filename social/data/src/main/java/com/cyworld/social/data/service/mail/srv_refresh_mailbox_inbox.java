package com.cyworld.social.data.service.mail;

import akka.actor.ActorRef;
import com.alibaba.fastjson.JSON;
import com.cyworld.social.data.actors.public_message_define;
import com.cyworld.social.data.entity.mail.*;
import com.cyworld.social.utils.request.mail.req_refresh_mailbox_inbox;
import com.cyworld.social.data.actors.mail.actor_mailbox;
import com.cyworld.social.data.actors.mail.actor_mailgroup;
import com.cyworld.social.data.actors.akka_system;
import com.cyworld.social.data.mongodb.db_context;
import com.udojava.evalex.Expression;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import java.util.stream.Collectors;

import static akka.pattern.Patterns.ask;

@Component
public class srv_refresh_mailbox_inbox {

    public static class context {
        req_refresh_mailbox_inbox req;
        ActorRef actor_mailbox_ref;
        Map<String, String> condition_values;
    }

    @Autowired
    akka_system akka;

    private List<mail_by_broadcast> filter_mail_by_broadcast(TreeSet<mail_by_broadcast> src, long time_stamp, Map<String, String> condition_values) {
        List<mail_by_broadcast> res = new ArrayList<>();
        if (src.isEmpty())
            return res;
        for (mail_by_broadcast mail : src) {
            if (mail.getTime_stamp() <= time_stamp)
                return res;
            if(mail.getCondition()==null)
                continue;
            if(mail.getCondition().length()==0)
                continue;
            Expression exp = new Expression(mail.getCondition());
            for (Map.Entry<String, String> entry : condition_values.entrySet()) {
                exp.setVariable(entry.getKey(), entry.getValue());
            }
            if (exp.eval().intValue() == 1)
                res.add(mail_by_broadcast.deep_clone(mail));
        }
        return res;
    }

    public Mono serv(ServerRequest request) {
        context this_context = new context();
        return request.bodyToMono(req_refresh_mailbox_inbox.class)
                .doOnNext(body -> {
                    this_context.req = body;
                    this_context.condition_values = Map.class.cast(JSON.parse(body.getValue_Json()));
                })
                .flatMap(body -> {
                    return actor_mailbox.find_or_build
                            (
                                    this_context.req.getNamespace(),
                                    this_context.req.getId(),
                                    akka.actor_system
                            );
                })
                .doOnNext(ref -> this_context.actor_mailbox_ref = ref)
                .flatMap(ref -> {
                    if (ref.isTerminated())
                        return Mono.error(new Throwable("cant create actor".concat(ref.path().name())));
                    else
                        return Mono.fromCompletionStage
                                (
                                        ask
                                                (
                                                        ref,
                                                        ((actor_mailbox.msg_readonly_inject_ask<mailbox,List<subscription>>) box -> {
                                                            return box
                                                                    .getMail_groups()
                                                                    .stream()
                                                                    .map(g -> subscription.deep_clone(g))
                                                                    .collect(Collectors.toList());
                                                        }),
                                                        Duration.ofSeconds(1)
                                                )
                                );
                })
                .flatMap(o -> {
                    List<subscription> list = List.class.cast(o);
                    return Flux.fromIterable(list)
                            .flatMap(e -> {
                                subscription su = subscription.class.cast(e);
                                return actor_mailgroup
                                        .find_or_build(su.getMailgroup_ID(),akka.actor_system)
                                        .flatMap(ref -> {
                                            return Mono.fromCompletionStage
                                                    (
                                                            ask
                                                                    (
                                                                            ref,
                                                                            ((actor_mailgroup
                                                                                    .msg_readonly_inject_ask<mailgroup,List<mail_by_broadcast>>)
                                                                                    grp -> {
                                                                                        if(grp.isOpening()) {
                                                                                            return filter_mail_by_broadcast(
                                                                                                    grp.getInbox(),
                                                                                                    su.getLast_get_timer(),
                                                                                                    this_context.condition_values
                                                                                            );
                                                                                        }
                                                                                        else {
                                                                                            actor_mailbox.unsubscribe(
                                                                                                    this_context.actor_mailbox_ref,
                                                                                                    su.getMailgroup_ID()
                                                                                            );
                                                                                            return new ArrayList<>();
                                                                                        }

                                                                                    }),
                                                                            Duration.ofSeconds(1)
                                                                    )
                                                                    .thenApply(List.class::cast)
                                                    );
                                        })
                                        .onErrorResume(err->{
                                            return Mono.just(new ArrayList<mail_by_broadcast>());
                                        });
                            })
                            .reduce((l1, l2) -> {
                                l1.addAll(l2);
                                return l1;
                            });
                })
                .flatMap(o -> {
                    List<mail_by_broadcast> list = List.class.cast(o);
                    if (list.isEmpty())
                        return Mono.just(Long.valueOf(0));
                    else
                        return Mono.fromCompletionStage
                                (
                                        ask
                                                (
                                                        this_context.actor_mailbox_ref,
                                                        ((actor_mailbox.msg_inject_ask<mailbox,Long>) box -> {
                                                            int mail_id = 1 + Integer.valueOf
                                                                    (
                                                                            box.getInbox()
                                                                                    .stream()
                                                                                    .map(m -> Integer.valueOf(m.getId()))
                                                                                    .max(Integer::compareTo)
                                                                                    .orElse(0)
                                                                    );
                                                            for (mail_by_broadcast mb : list) {
                                                                box.getInbox().add
                                                                        (
                                                                                mail_by_personal
                                                                                        .builder()
                                                                                        .id(String.valueOf(mail_id))
                                                                                        .id_box(box.get_id())
                                                                                        .id_namespace(box.getNamespace())
                                                                                        .time_stamp(System.currentTimeMillis())
                                                                                        .type(mb.getType())
                                                                                        .from(mb.getFrom())
                                                                                        .title(mb.getTitle())
                                                                                        .content(mb.getContent())
                                                                                        .attachment(mb.getAttachment())
                                                                                        .build()
                                                                        );
                                                                for (subscription su : box.getMail_groups()) {
                                                                    if (su.getMailgroup_ID().equals(mb.getId_group()) &&
                                                                            su.getLast_get_timer() < mb.getTime_stamp())
                                                                        su.setLast_get_timer(mb.getTime_stamp());
                                                                }
                                                                mail_id++;
                                                            }
                                                            return box.getInbox().stream().filter(m -> !m.is_read()).count();
                                                        }),
                                                        Duration.ofSeconds(1)
                                                )
                                                .thenApply(Long.class::cast)
                                );

                })
                .flatMap(count -> {
                    return ServerResponse.ok().body(BodyInserters.fromObject(count));
                })
                .doOnSuccess(unused -> {
                    this_context.actor_mailbox_ref.tell(public_message_define.SAVE_SIGNAL, ActorRef.noSender());
                });
    }
}
