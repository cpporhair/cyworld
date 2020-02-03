package com.cyworld.social.test;

import com.google.common.collect.Lists;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Scheduler;
import reactor.core.scheduler.Schedulers;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import static java.util.concurrent.Executors.newScheduledThreadPool;

@SpringBootTest
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class test_1 {
    @Test
    public void test_001_Mono()throws Exception{
        //新建两个线程池
        ExecutorService s1=Executors.newFixedThreadPool(10);
        ExecutorService s2=Executors.newFixedThreadPool(1);
        for(int A=0;A<100;++A)
            Mono.just(1)
                    //这里开始受到subscribeOn影响,并发在s1里
                    .doOnNext(i->{
                        System.out.println("1 : "+Thread.currentThread().getName());
                    })
                    .map(i->i+1)
                    .publishOn(Schedulers.fromExecutor(s2))
                    //这里开始受到publishOn的影响之后并发在s2里
                    .doOnNext(i->{
                        System.out.println("2 : "+Thread.currentThread().getName());
                    })
                    .publishOn(Schedulers.fromExecutor(s1))
                    //这里开始受到publishOn的影响之后并发在s1里
                    .doOnNext(i->{
                        System.out.println("3 : "+Thread.currentThread().getName());
                    })
                    .subscribeOn(Schedulers.fromExecutor(s1))
                    .subscribe();
        Thread.sleep(1000);
    }
    @Test
    public void test_002_Flux()throws Exception{
        List<Integer> l=new ArrayList<>();
        for(int i=0;i<100;i++)
            l.add(i);
        Flux.fromIterable(l)
                //这里开始并发--->runOn的线程池里
                .parallel()
                .runOn(
                        //创建弹性线程池,和test_001一样,都是不同线程池的创建方式
                        Schedulers.elastic()
                )
                .doOnNext(i->{
                    System.out.println(i+" : "+Thread.currentThread().getName());
                })
                //这里开始回到合并到runOn的线程池中的一个线程
                .sequential()
                .doOnNext(i->{
                    System.out.println(i+" : "+Thread.currentThread().getName());
                })
                //这里又转到一个新建的线程AAA
                .publishOn(Schedulers.newSingle("AAA"))
                .doOnNext(i->{
                    System.out.println(i+" : "+Thread.currentThread().getName());
                })
                .subscribe();
        Thread.sleep(5000);
    }
}
