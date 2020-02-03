package com.cyworld.social.test;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.cyworld.social.utils.request.friend.*;
import com.cyworld.social.utils.request.global.req_new_server;
import com.cyworld.social.utils.request.global.req_new_user;
import com.cyworld.social.utils.request.guild.req_new_guild;
import com.cyworld.social.utils.request.mail.req_del_mail;
import com.cyworld.social.utils.request.mail.req_get_mail_detail;
import com.cyworld.social.utils.request.mail.req_new_broadcast_mail;
import com.cyworld.social.utils.request.mail.req_refresh_and_get_mail_list;
import com.cyworld.social.utils.request.ranks.req_add_ranks_data;
import com.cyworld.social.utils.request.ranks.req_get_ranks;
import com.cyworld.social.utils.response.friend.res_one_friend_with_simple_info;
import com.cyworld.social.utils.response.friend.res_one_get_help_data;
import com.cyworld.social.utils.response.mail.res_get_mail_detail;
import com.cyworld.social.utils.response.mail.res_get_mail_list;
import com.mongodb.reactivestreams.client.MongoClient;
import com.mongodb.reactivestreams.client.MongoClients;
import com.netflix.util.Pair;
import lombok.Data;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

import static java.lang.Math.abs;
import static org.springframework.web.reactive.function.client.WebClient.create;

@SpringBootTest
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class test_base_env_init {
    private static final Logger logger = LoggerFactory.getLogger(test_base_env_init.class.getName());
    static String hx_access_token;
    static String hx_app_uuid;
    static String hx_token_extime;
    String hx_base_path="http://a1.easemob.com/1105190313097510/cytalk";
    @Test
    //@Ignore
    public void t_000_init_huanxin(){
        Mono.just(1)
                .flatMap(i->{
                    JSONObject req=new JSONObject();
                    req.put("grant_type","client_credentials");
                    req.put("client_id","YXA6Gs6ToEWKEemlefuE7RzA0g");
                    req.put("client_secret","YXA6Qni92x-1wPBqwQAPJrKOJNdzLW4");

                    return WebClient.create()
                            .post()
                            .uri(hx_base_path.concat("/token"))
                            .contentType(MediaType.APPLICATION_JSON)
                            .body(BodyInserters.fromObject(JSON.toJSON(req)))
                            .exchange()
                            .flatMap(res->{
                                switch(res.statusCode()){
                                    case OK:
                                        return res.bodyToMono(String.class)
                                                .map(body -> JSON.parseObject(body));
                                    default:
                                        return Mono.error(new Throwable("aaaa"));
                                }
                            });
                })
                .doOnNext(json->{
                    hx_access_token=json.getString("access_token");
                    hx_token_extime=String.valueOf(System.currentTimeMillis() + Long.valueOf(json.getString("expires_in")) * 1000);
                    hx_app_uuid=json.getString("application");
                })
                .block();
    }

    @Test
    public void t_001_clear_env_for_test(){
        static_data.init();

        MongoClient c= MongoClients.create("mongodb://localhost:27017");
        Flux.from(c.getDatabase("mail")
                .listCollectionNames())
                .flatMap(s->{
                    return Mono.from(c.getDatabase("mail").getCollection(s).drop());
                })
                .count().block();

        Flux.from(c.getDatabase("friend")
                .listCollectionNames())
                .flatMap(s->{
                    return Mono.from(c.getDatabase("friend").getCollection(s).drop());
                })
                .log()
                .count().block();

        Flux.from(c.getDatabase("ranks")
                .listCollectionNames())
                .flatMap(s->{
                    return Mono.from(c.getDatabase("ranks").getCollection(s).drop());
                })
                .count().block();
        Flux.from(c.getDatabase("guild")
                .listCollectionNames())
                .flatMap(s->{
                    return Mono.from(c.getDatabase("guild").getCollection(s).drop());
                })
                .count().block();

        Mono.from(c.getDatabase("mail").createCollection("mailgroup")).block();

        WebClient.create()
                .get()
                .uri("http://10.0.0.11:8070/inner_data/v1/clear")
                //.uri("http://www.baidu.com")
                .exchange()
                .flatMap(res->{
                    switch(res.statusCode()){
                        case OK:
                            return Mono.just(true);
                        default:
                            return res.bodyToMono(String.class).flatMap(s->Mono.error(new Throwable(s)));
                    }
                }).block();
    }

    @Test
    public void t_002_create_gameserver(){
        Flux.fromIterable(static_data.servers)
                .map(server->{
                    req_new_server o= new req_new_server();
                    o.id=server.name;
                    o.setDescription("game_server.".concat(server.name));
                    return o;
                })
                .flatMap(v->{
                    return create()
                            .post()
                            .uri("http://10.0.0.11:8080/global/v1/gameserver")
                            .contentType(MediaType.APPLICATION_JSON)
                            .body(BodyInserters.fromObject(v))
                            .exchange()
                            .flatMap(res->{
                                switch(res.statusCode()){
                                    case OK:
                                        return Mono.just(true);
                                    default:
                                        return res
                                                .bodyToMono(String.class)
                                                .doOnNext(s->{logger.error(s);})
                                                .flatMap(s->Mono.error(new Throwable(s)))
                                                .onErrorReturn(false);
                                }
                            });
                })
                .count()
                .block();
    }

    @Test
    public void t_003_create_user(){
        Flux.fromIterable(static_data.players)
                .flatMap(player ->{
                    req_new_user new_user=new req_new_user();
                    new_user.setNamespace(player.server_name);
                    new_user.setNickname(player.player_id);
                    new_user.setPlayer_id(player.player_id);
                    new_user.setPassword(player.player_id);
                    return create()
                            .post()
                            .uri("http://10.0.0.11:8080/global/v1/user")
                            .contentType(MediaType.APPLICATION_JSON)
                            .body(BodyInserters.fromObject(new_user))
                            .exchange();
                })
                .flatMap(res->{
                    switch (res.statusCode()){
                        case OK:
                            return Mono.just(true);
                        default:
                            return res
                                    .bodyToMono(String.class)
                                    .doOnNext(err->{logger.error(err);})
                                    .flatMap(err->Mono.error(new Throwable(err)));

                    }
                })
                .count()
                .block();
    }
    @Test
    public void t_004_test_transaction(){
        @Data
        class temp_place_holder{
            String _id="t001";
            String server="s003";
            String tmp="AAA";
        }

        temp_place_holder t=new temp_place_holder();


        String j=JSON.toJSONString(t);
        MongoClient c= MongoClients.create("mongodb://localhost:27017");
        ReactiveMongoTemplate Template=new ReactiveMongoTemplate(c,"friend");
        Template.save(t,t.server).block();


        Mono.just(t)
                .flatMap(v->{
                    req_new_user new_user=new req_new_user();
                    new_user.setNamespace(v.server);
                    new_user.setNickname(v._id);
                    new_user.setPlayer_id(v._id);
                    new_user.setPassword(v._id);
                    return create()
                            .post()
                            .uri("http://10.0.0.11:8080/global/v1/user")
                            .contentType(MediaType.APPLICATION_JSON)
                            .body(BodyInserters.fromObject(new_user))
                            .exchange();
                })
                .flatMap(res->{
                    switch (res.statusCode()){
                        case OK:
                            return res
                                    .bodyToMono(String.class)
                                    .doOnNext(err->{logger.error(err);})
                                    .flatMap(err->Mono.error(new Throwable(err)));
                        default:
                            return Mono.just(true);

                    }
                })
                .block();
    }

    @Test
    public void t_005_test_send_server_mail(){
        @Data
        class mail_attachment{
            String id;
            String content;
            mail_attachment(String id,String content){
                this.id=id;this.content=content;
            }
        }
        String[] attachment=new String[2];
        attachment[0]=(JSON.toJSONString(new mail_attachment("001","AAA")));
        attachment[1]=(JSON.toJSONString(new mail_attachment("002","BBB")));



        Flux.fromIterable(static_data.servers)
                .map(s->{
                    req_new_broadcast_mail req=new req_new_broadcast_mail();

                    req.setAttechmentJson(attachment);
                    req.setContent("Test Mail");
                    req.setCondition("level>20&&regtime>200");
                    req.setFrom("t_005_test_send_server_mail");
                    req.setTitle("a mail");
                    req.setType("1");
                    req.setId_group(s.name);
                    return req;
                })
                .parallel()
                .runOn(Schedulers.elastic())
                .flatMap(req->{
                    return WebClient
                            .create()
                            .post()
                            .uri("http://10.0.0.11:8080/mail/v1/broadcast")
                            .contentType(MediaType.APPLICATION_JSON)
                            .body(BodyInserters.fromObject(req))
                            .exchange();
                })
                .flatMap(res->{
                    switch (res.statusCode()){
                        case OK:
                            return Mono.just(true);
                        default:
                            return res
                                    .bodyToMono(String.class)
                                    .doOnNext(err->{logger.error(err);})
                                    .flatMap(err->Mono.error(new Throwable(err)));
                    }
                })
                .sequential()
                .repeat(3)
                .count()
                .block();
    }
    @Test
    public void t_006_test_get_mail_simple(){
        @Data
        class mailConditionValue{
            String level;
            String regtime;
            String other;
        }


        Flux.fromIterable(static_data.players)
                .take(1)
                .map(user->{
                    req_refresh_and_get_mail_list req=new req_refresh_and_get_mail_list();
                    req.setId(user.player_id);
                    req.setNamesapce(user.server_name);
                    req.setSkip("0");
                    req.setTake("1");
                    mailConditionValue value=new mailConditionValue();
                    value.level="35";
                    if(user.player_id.equals("u002"))
                        value.regtime="100";
                    else
                        value.regtime="1000";
                    value.other="123";
                    req.setValue_Json(JSON.toJSONString(value));
                    return req;
                })
                .flatMap(req->{
                    return WebClient
                            .create()
                            .post()
                            .uri("http://10.0.0.11:8080/mail/v1/getmaillist")
                            .contentType(MediaType.APPLICATION_JSON)
                            .body(BodyInserters.fromObject(req))
                            .exchange()
                            .flatMap(res->{
                                switch (res.statusCode()){
                                    case OK:
                                        return res
                                                .bodyToMono(String.class)
                                                .doOnNext(err->{logger.error(err);})
                                                .map(str->{
                                                    Pair<req_refresh_and_get_mail_list,res_get_mail_list> p
                                                            =new Pair<req_refresh_and_get_mail_list,res_get_mail_list>
                                                            (
                                                                    req,
                                                                    JSONObject.parseObject(str, res_get_mail_list.class)
                                                            );
                                                    return p;
                                                });
                                    default:
                                        return res
                                                .bodyToMono(String.class)
                                                .doOnNext(err->{logger.error(err);})
                                                .flatMap(err->Mono.error(new Throwable(err)));
                                }
                            });
                })
                .flatMap(res->{
                    if(res.first().getId().equals("u002")){
                        if(res.second().getMails().size()>0)
                            return Mono.error(new Throwable("res.second().size()>0"));
                        return Mono.empty();
                    }
                    else{
                        if(res.second().getMails().size()!=1)
                            return Mono.error(new Throwable("res.second().size()!=1"));
                        req_get_mail_detail req=new req_get_mail_detail();
                        req.setId(res.first().getId());
                        req.setNamesapce(res.first().getNamesapce());
                        req.setMail_id(res.second().getMails().get(0).getId());
                        return WebClient
                                .create()
                                .post()
                                .uri("http://10.0.0.11:8080/mail/v1/detail")
                                .contentType(MediaType.APPLICATION_JSON)
                                .body(BodyInserters.fromObject(req))
                                .exchange();
                    }

                })
                .flatMap(res->{
                    switch (res.statusCode()){
                        case OK:
                            return res
                                    .bodyToMono(String.class)
                                    .map(str->{
                                        return JSONObject.parseObject(str, res_get_mail_detail.class);
                                    })
                                    .flatMap(detail->{
                                        if(detail.getContent().equals("Test Mail"))
                                            return Mono.just(detail.getId());
                                        else
                                            return res
                                                    .bodyToMono(String.class)
                                                    .doOnNext(err->{logger.error(err);})
                                                    .flatMap(err->Mono.error(new Throwable(err)));
                                    });
                        default:
                            return res
                                    .bodyToMono(String.class)
                                    .doOnNext(err->{logger.error(err);})
                                    .flatMap(err->Mono.error(new Throwable(err)));
                    }

                })
                .index((i,str)->{
                    return static_data.players.get(i.intValue());
                })
                .flatMap(user->{
                    req_del_mail req=new req_del_mail();
                    req.setId(user.player_id);
                    req.setNamesapce(user.server_name);
                    req.setMail_id_list("1;2;3");

                    return WebClient
                            .create()
                            .post()
                            .uri("http://10.0.0.11:8080/mail/v1/del_mail")
                            .contentType(MediaType.APPLICATION_JSON)
                            .body(BodyInserters.fromObject(req))
                            .exchange()
                            .flatMap(clientResponse -> {
                                switch (clientResponse.statusCode()){
                                    case OK:
                                        return Mono.just(1);
                                    default:
                                        return clientResponse
                                                .bodyToMono(String.class)
                                                .doOnNext(err->{logger.error(err);})
                                                .flatMap(err->Mono.error(new Throwable(err)));
                                }
                            });
                })
                .count()
                .block();

    }

    @Test
    public void t_007_test_friend_reject(){

        Flux.just(static_data.players.get(1),static_data.players.get(2))
                .flatMap(o->{
                    req_friends_invite req=new req_friends_invite();
                    req.setFrom_id(static_data.players.get(0).player_id);
                    req.setFrom_namespace(static_data.players.get(0).server_name);
                    req.setTarget_id(o.player_id);
                    req.setTarget_namespace(o.server_name);

                    return WebClient.create()
                            .post()
                            .uri("http://10.0.0.11:8080/friend/v1/invite")
                            .contentType(MediaType.APPLICATION_JSON)
                            .body(BodyInserters.fromObject(req))
                            .exchange()
                            .flatMap(res->{
                                switch (res.statusCode()){
                                    case OK:
                                        return Mono.just(o);
                                    default:
                                        return res
                                                .bodyToMono(String.class)
                                                .doOnNext(err->{logger.error(err);})
                                                .flatMap(err->Mono.error(new Throwable(err)));
                                }
                            });
                })
                .flatMap(o->{
                    req_friends_reject req=new req_friends_reject();
                    req.setFriend_id(static_data.players.get(0).player_id);
                    req.setFriend_namespace(static_data.players.get(0).server_name);
                    req.setPlayer_id(o.player_id);
                    req.setPlayer_namespace(o.server_name);
                    return WebClient.create()
                            .post()
                            .uri("http://10.0.0.11:8080/friend/v1/reject")
                            .contentType(MediaType.APPLICATION_JSON)
                            .body(BodyInserters.fromObject(req))
                            .exchange()
                            .flatMap(res->{
                                switch (res.statusCode()){
                                    case OK:
                                        return Mono.just(o);
                                    default:
                                        return res
                                                .bodyToMono(String.class)
                                                .doOnNext(err->{logger.error(err);})
                                                .flatMap(err->Mono.error(new Throwable(err)));
                                }
                            });
                })

                .flatMap(o->{
                    req_list_invite req=new req_list_invite();
                    req.setPlayer_id(o.player_id);
                    req.setPlayer_namespace(o.server_name);
                    return WebClient.create()
                            .post()
                            .uri("http://10.0.0.11:8080/friend/v1/list_invite")
                            .contentType(MediaType.APPLICATION_JSON)
                            .body(BodyInserters.fromObject(req))
                            .exchange()
                            .flatMap(res->{
                                switch (res.statusCode()){
                                    case OK:
                                        return res.bodyToMono(String.class)
                                                .flatMap(str->{
                                                    List<String> r= JSON.parseArray(str, String.class);
                                                    if(r.isEmpty())
                                                        return Mono.just(o);
                                                    else
                                                        return Mono.error(new Throwable("error for list_invite size"));
                                                });
                                    default:
                                        return res
                                                .bodyToMono(String.class)
                                                .doOnNext(err->{logger.error(err);})
                                                .flatMap(err->Mono.error(new Throwable(err)));
                                }
                            });
                })
                .count().block();

    }

    @Test
    public void t_008_test_friend_apply(){

        Flux.just(static_data.players.get(1),static_data.players.get(2))
                .flatMap(o->{
                    req_friends_invite req=new req_friends_invite();
                    req.setFrom_id(static_data.players.get(0).player_id);
                    req.setFrom_namespace(static_data.players.get(0).server_name);
                    req.setTarget_id(o.player_id);
                    req.setTarget_namespace(o.server_name);

                    return WebClient.create()
                            .post()
                            .uri("http://10.0.0.11:8080/friend/v1/invite")
                            .contentType(MediaType.APPLICATION_JSON)
                            .body(BodyInserters.fromObject(req))
                            .exchange()
                            .flatMap(res->{
                                switch (res.statusCode()){
                                    case OK:
                                        return Mono.just(o);
                                    default:
                                        return res
                                                .bodyToMono(String.class)
                                                .doOnNext(err->{logger.error(err);})
                                                .flatMap(err->Mono.error(new Throwable(err)));
                                }
                            });
                })
                .flatMap(o->{
                    req_friends_apply req=new req_friends_apply();
                    req.setFriend_id(static_data.players.get(0).player_id);
                    req.setFriend_namespace(static_data.players.get(0).server_name);
                    req.setPlayer_id(o.player_id);
                    req.setPlayer_namespace(o.server_name);
                    return WebClient.create()
                            .post()
                            .uri("http://10.0.0.11:8080/friend/v1/apply")
                            .contentType(MediaType.APPLICATION_JSON)
                            .body(BodyInserters.fromObject(req))
                            .exchange()
                            .flatMap(res->{
                                switch (res.statusCode()){
                                    case OK:
                                        return Mono.just(o);
                                    default:
                                        return res
                                                .bodyToMono(String.class)
                                                .doOnNext(err->{logger.error(err);})
                                                .flatMap(err->Mono.error(new Throwable(err)));
                                }
                            });
                })
                .flatMap(o->{
                    req_list_friends req=new req_list_friends();
                    req.setPlayer_id(static_data.players.get(0).player_id);
                    req.setPlayer_namespace(static_data.players.get(0).server_name);
                    return WebClient.create()
                            .post()
                            .uri("http://10.0.0.11:8080/friend/v1/list_friends")
                            .contentType(MediaType.APPLICATION_JSON)
                            .body(BodyInserters.fromObject(req))
                            .exchange()
                            .flatMap(res->{
                                switch (res.statusCode()){
                                    case OK:
                                        return res.bodyToMono(String.class)
                                                .map(str->{
                                                    List<String> r= JSON
                                                            .parseArray(str, String.class);
                                                    return r;
                                                })
                                                .map(lst->{
                                                    return lst.stream()
                                                            .map(obj->{
                                                                return JSON.parseObject(obj.toString(),res_one_friend_with_simple_info.class);
                                                            })
                                                            .collect(Collectors.toList());
                                                })
                                                .flatMap(lst->{
                                                    if(lst.stream().anyMatch(e->{
                                                        return e.get_id().equals(o.player_id);
                                                    }))
                                                        return Mono.just(o);
                                                    else
                                                        return Mono.error(new Throwable("not firend ".concat(o.player_id)));
                                                });

                                    default:
                                        return res
                                                .bodyToMono(String.class)
                                                .doOnNext(err->{logger.error(err);})
                                                .flatMap(err->Mono.error(new Throwable(err)));
                                }
                            });
                })
                .count()
                .block();
    }

    private void random_rank(String type,int count){
        List<req_add_ranks_data> lst=new ArrayList<>();

        Random rdm=new Random(System.nanoTime());

        for(int i=0;i<count;++i){
            req_add_ranks_data r=new req_add_ranks_data();
            int d=abs(rdm.nextInt())%10;
            logger.error(String.valueOf(d));
            r.setCompare_value(d);
            r.setData(String.valueOf(d));
            r.setType(type);
            r.setID(String.valueOf(i));
            r.setMax_count(10);
            lst.add(r);
        }
        if(count>1){
            for(int i=0;i<10;++i){
                req_add_ranks_data r=new req_add_ranks_data();
                r.setCompare_value(999);
                r.setData(String.valueOf(999));
                r.setType(type);
                r.setID("1");
                r.setMax_count(10);
                lst.add(r);
            }
        }


        Flux.fromIterable(lst)
                .flatMap(req->{
                    return WebClient.create()
                            .post()
                            .uri("http://10.0.0.11:8080/rank/v1/add_value")
                            .contentType(MediaType.APPLICATION_JSON)
                            .body(BodyInserters.fromObject(req))
                            .exchange()
                            .flatMap(res->{
                                switch (res.statusCode()){
                                    case OK:
                                        logger.error("!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
                                        return Mono.just(req);
                                    default:
                                        return res
                                                .bodyToMono(String.class)
                                                .doOnNext(err->{logger.error(err);})
                                                .flatMap(err->Mono.error(new Throwable(err)));
                                }
                            });
                })
                .doOnError(e->{logger.error(e.getMessage());})
                .count()
                .block();
    }

    @Test
    public void t_009_test_rank(){
        random_rank("1",1);
        random_rank("1",50);
        req_get_ranks rgr=new req_get_ranks();
        rgr.setType("1");
        rgr.setSkip(0);
        rgr.setTake(100);
        Mono.just(rgr)
                .flatMap(req->{
                    return WebClient.create()
                            .post()
                            .uri("http://10.0.0.11:8080/rank/v1/get_ranks")
                            .contentType(MediaType.APPLICATION_JSON)
                            .body(BodyInserters.fromObject(req))
                            .exchange()
                            .flatMap(res->{
                                switch (res.statusCode()){
                                    case OK:
                                        return res.bodyToMono(List.class)
                                                .doOnNext(l->{
                                                    l.stream().forEach(s->{
                                                        logger.error(s.toString());
                                                    });
                                                });
                                    default:
                                        return res
                                                .bodyToMono(String.class)
                                                .doOnNext(err->{logger.error(err);})
                                                .flatMap(err->Mono.error(new Throwable(err)));
                                }
                            });
                })
                .block();
    }
    @Test
    public void t_010_test_friend_help(){

        Flux.just(1,2)
                .flatMap(i->{
                    req_issue_help req=new req_issue_help();
                    req.setAmount(i);
                    req.setLifecycle(1000*600);
                    req.setNeed_ID("0909");
                    req.setPlayer_id(static_data.players.get(i).player_id);
                    req.setPlayer_namespace(static_data.players.get(i).server_name);
                    return WebClient.create()
                            .post()
                            .uri("http://10.0.0.11:8080/friend/v1/issue_help")
                            .contentType(MediaType.APPLICATION_JSON)
                            .body(BodyInserters.fromObject(req))
                            .retrieve()
                            .onStatus(HttpStatus::isError,res->{
                                return res
                                        .bodyToMono(String.class)
                                        .doOnNext(err->{logger.error(err);})
                                        .flatMap(err->Mono.error(new Throwable(err)));
                            })
                            .bodyToMono(String.class)
                            .doOnNext(logger::info);
                })
                .count()
                .block();

        Mono.just(0)
                .flatMap(i->{
                    req_list_help req=new req_list_help();
                    req.setPlayer_id(static_data.players.get(i).player_id);
                    req.setPlayer_namespace(static_data.players.get(i).server_name);
                    return WebClient.create()
                            .post()
                            .uri("http://10.0.0.11:8080/friend/v1/list_help")
                            .contentType(MediaType.APPLICATION_JSON)
                            .body(BodyInserters.fromObject(req))
                            .retrieve()
                            .onStatus(HttpStatus::isError,res->{
                                return res
                                        .bodyToMono(String.class)
                                        .doOnNext(err->{logger.error(err);})
                                        .flatMap(err->Mono.error(new Throwable(err)));
                            })
                            .bodyToMono(String.class)
                            .doOnNext(logger::info)
                            .map(res->{
                                return JSON.parseArray(res,String.class);
                            });
                })
                .flatMap(lst->{
                    if(lst.isEmpty())
                        return Mono.empty();
                    res_one_get_help_data last_res=JSON.parseObject(lst.get(0),res_one_get_help_data.class);
                    req_do_help req=new req_do_help();
                    req.setAmount(1);
                    req.setNeed_ID(last_res.getHelp_type());
                    req.setAmount(last_res.getHelp_amount());
                    req.setPlayer_id(static_data.players.get(0).player_id);
                    req.setPlayer_namespace(static_data.players.get(0).server_name);
                    req.setTarget_id(last_res.getFrined_id());
                    req.setTarget_namespace(last_res.getFriend_namespace());
                    return WebClient.create()
                            .post()
                            .uri("http://10.0.0.11:8080/friend/v1/do_help")
                            .contentType(MediaType.APPLICATION_JSON)
                            .body(BodyInserters.fromObject(req))
                            .retrieve()
                            .onStatus(HttpStatus::isError,res->{
                                return res
                                        .bodyToMono(String.class)
                                        .doOnNext(err->{logger.error(err);})
                                        .flatMap(err->Mono.error(new Throwable(err)));
                            })
                            .bodyToMono(String.class)
                            .switchIfEmpty(Mono.just("1111"))
                            .doOnNext(res->{
                                logger.error(res);
                            })
                            .log();
                })
                .block();
    }

    @Test
    public void t_011_test_friend_blacklist(){
        Mono.just(1)
                .flatMap(i->{
                    req_add_to_blacklist req=new req_add_to_blacklist();
                    req.setFriend_id(static_data.players.get(0).player_id);
                    req.setFriend_namespace(static_data.players.get(0).server_name);
                    req.setPlayer_id(static_data.players.get(i).player_id);
                    req.setPlayer_namespace(static_data.players.get(i).server_name);
                    return WebClient.create()
                            .post()
                            .uri("http://10.0.0.11:8080/friend/v1/add_to_blacklist")
                            .contentType(MediaType.APPLICATION_JSON)
                            .body(BodyInserters.fromObject(req))
                            .retrieve()
                            .onStatus(HttpStatus::isError,res->{
                                return res
                                        .bodyToMono(String.class)
                                        .doOnNext(err->{logger.error(err);})
                                        .flatMap(err->Mono.error(new Throwable(err)));
                            })
                            .bodyToMono(String.class)
                            .map(str->i);
                })
                .flatMap(i->{
                    req_add_to_blacklist req=new req_add_to_blacklist();
                    req.setFriend_id(static_data.players.get(0).player_id);
                    req.setFriend_namespace(static_data.players.get(0).server_name);
                    req.setPlayer_id(static_data.players.get(i).player_id);
                    req.setPlayer_namespace(static_data.players.get(i).server_name);
                    return WebClient.create()
                            .post()
                            .uri("http://10.0.0.11:8080/friend/v1/remove_from_blacklist")
                            .contentType(MediaType.APPLICATION_JSON)
                            .body(BodyInserters.fromObject(req))
                            .retrieve()
                            .onStatus(HttpStatus::isError,res->{
                                return res
                                        .bodyToMono(String.class)
                                        .doOnNext(err->{logger.error(err);})
                                        .flatMap(err->Mono.error(new Throwable(err)));
                            })
                            .bodyToMono(String.class)
                            .doOnSuccess(str->{
                                logger.info(str);
                            })
                            .map(str->i);
                })
                .block();
    }
    @Test
    //@Ignore
    public void t_012_create_guild(){
        Flux.fromIterable(static_data.players)
                .filter(player->{
                    return player.player_id.equals("u001");
                })
                .map(player->{
                    req_new_guild req=new req_new_guild();
                    req.setDomain_id("0");
                    req.setLanguage("BD");
                    req.setDeclaration("1111");
                    req.setFlag("1");
                    req.setLevel_for_join(20);
                    req.setMax_member_count(20);
                    req.setName(player.server_name.concat(".").concat(player.player_id));
                    switch (player.server_name){
                        case "s001":
                        case "s002":
                            req.setNeed_apply_when_join(true);
                            break;
                        default:
                            req.setNeed_apply_when_join(false);
                            break;
                    }
                    req.setOwner_power(700);
                    req.setPlayer_id(player.player_id);
                    req.setPlayer_namespace(player.server_name);
                    req.setNotice("2222");
                    return req;
                })
                .flatMap(req->{
                    return WebClient.create()
                            .post()
                            .uri("http://10.0.0.11:8080/guild/v1/new_guild")
                            .contentType(MediaType.APPLICATION_JSON)
                            .body(BodyInserters.fromObject(JSON.toJSON(req)))
                            .exchange()
                            .flatMap(clientResponse -> {
                                switch (clientResponse.statusCode()){
                                    case OK:
                                        return clientResponse.bodyToMono(String.class)
                                                .doOnNext(str->{
                                                    logger.info(str);
                                                })
                                                .map(str->true);
                                    default:
                                        return clientResponse
                                                .bodyToMono(String.class)
                                                .doOnNext(str->{
                                                    logger.error(str);
                                                })
                                                .map(str->false);
                                }
                            });
                })
                .all(res->res)
                .flatMap(res->{
                    if(res)
                        return Mono.empty();
                    else
                        return Mono.error(new Throwable());
                })
                .block();
    }
}
