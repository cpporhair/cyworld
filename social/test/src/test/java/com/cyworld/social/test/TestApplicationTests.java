package com.cyworld.social.test;

import com.alibaba.fastjson.JSON;
import com.alibaba.nacos.client.config.utils.MD5;
import com.cyworld.social.utils.request.global.req_new_server;
import com.cyworld.social.utils.response.guild.res_get_members;
import com.cyworld.social.utils.response.guild.res_get_members_component;
import com.cyworld.social.utils.response.guild.res_join;
import com.google.common.base.Strings;
import com.google.common.collect.Iterables;
import com.google.common.primitives.UnsignedBytes;
import com.mongodb.reactivestreams.client.MongoClient;
import com.mongodb.reactivestreams.client.MongoClients;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.codec.Hex;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.UnicastProcessor;
import reactor.core.scheduler.Schedulers;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@SpringBootTest
public class TestApplicationTests {
    private static final Logger logger = LoggerFactory.getLogger(TestApplicationTests.class.getName());

    public Mono<String> aaa(boolean v) {
        if(v)
            return Mono.just("b").delayElement(Duration.ofSeconds(1)).doOnNext(logger::info);
        else
            return Mono.just("a").doOnNext(logger::info).flatMap(o->Mono.empty());

    }

    ArrayList<String> ll=new ArrayList<>();

    private List<String> kkk(){
        ll.add("1111");
        return ll;
    }

    @Test
    public void t000() throws Exception {

        Mono.just(1)
                .flatMap(i->{
                    if(1==1)
                        return Mono.empty();
                    else
                        return Mono.just(i+3);
                })
                .map(i->{
                    return true;
                })
                .switchIfEmpty(
                        Mono.just(false)
                )
                .doOnNext(b->{
                    System.out.println(b);
                })
                .block();

        String as="常委二哥.常委二哥常委二哥常委二哥常委二哥";
        int bs=as.hashCode();
        byte[] s=MD5.getInstance().hash(as);
        String ddd=String.valueOf(Hex.encode(as.getBytes()));
        StringBuilder b=new StringBuilder();
        for(int i=0;i<s.length;++i){


            b.append(UnsignedBytes.toString(s[i]));
        }
        String aa=String.valueOf(s);
        //String aa=Base64.getEncoder().encode(s).toString();

    }


}
