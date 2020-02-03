package com.cyworld.social.data;

import com.alibaba.fastjson.JSON;
import com.cyworld.social.data.actors.akka_system;
import com.cyworld.social.data.mongodb.db_context;
import com.udojava.evalex.Expression;
import lombok.Builder;
import lombok.Data;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.*;

@Component
public class test1 {

    @Autowired
    akka_system akka;

    @Autowired
    db_context mongo_context;

    @Data
    @Builder
    public static class uuu implements Comparable {
        String sdl;

        @Override
        public int compareTo(Object o) {
            return sdl.compareTo(uuu.class.cast(o).sdl);
        }
    }

    @Data
    public static class vvv {
        String id;
        TreeSet<uuu> popo = new TreeSet<>();
    }

    //@PostConstruct
    public void init() {


        mongo_context.mongo_templates.get("mail").getTemplate().updateFirst(
                Query.query(Criteria.where("_id").is("1aaaa").and("inbox._id").is("6")),
                new Update().set("inbox.$", "opop"),
                "server_0003"
        ).doOnNext(s -> {
            System.out.println(s.getModifiedCount());
        }).block();

        vvv v = new vvv();
        v.id = "1";
        v.popo.add(uuu.builder().sdl("A").build());
        v.popo.add(uuu.builder().sdl("B").build());
        v.popo.add(uuu.builder().sdl("C").build());
        v.popo.add(uuu.builder().sdl("D").build());

        mongo_context.mongo_templates.get("mail").getTemplate().save(v, "VP")
                .block();
        vvv v2
                = mongo_context.mongo_templates.get("mail").getTemplate().findOne(Query.query(Criteria.where("_id").is("1")), vvv.class, "VP")
                .block();


        Map<String, String> k = new HashMap<>();
        k.put("level", "1");
        k.put("name", "1");
        k.put("idid", "1");
        k.put("time", String.valueOf(System.currentTimeMillis()));

        String s = JSON.toJSON(k).toString();

        Map<String, String> a = Map.class.cast(JSON.parse(s));

        Expression expression = new Expression("level<1&&yuo<9090&&(a+b)ssss");
        boolean bp = expression.isBoolean();
        List<String> l = expression.getUsedVariables();
        expression.setVariable("level", "0");
        BigDecimal b = expression.eval();
        s = b.toString();
    }
}
