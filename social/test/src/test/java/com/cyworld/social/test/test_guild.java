package com.cyworld.social.test;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import com.alibaba.fastjson.JSON;
import com.cyworld.social.utils.request.guild.*;
import com.cyworld.social.utils.response.guild.res_get_ask_list_guild;
import com.cyworld.social.utils.response.guild.res_get_player_guild;
import lombok.Data;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;
//import org.slf4j.LoggerFactory;
import org.slf4j.LoggerFactory;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.ArrayList;

@SpringBootTest
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class test_guild {
    private static final LoggerContext loggerContext= (LoggerContext) LoggerFactory.getILoggerFactory();
    //Logger logger=loggerContext.getLogger("root");
    private static final Logger logger = loggerContext.getLogger("root");
    @Test
    public void a_000_get_guild_id(){
        logger.setLevel(Level.INFO);
        static_data.init();
        Flux.fromIterable(static_data.players)
                .filter(player->{
                    return player.player_id.equals("u001");
                })
                .flatMap(player -> {
                    req_get_player_guild req=new req_get_player_guild();
                    req.setPlayer_namespace(player.server_name);
                    req.setPlayer_id(player.player_id);
                    return WebClient.create()
                            .post()
                            .uri("http://10.0.0.11:8080/guild/v1/get_player_guild")
                            .contentType(MediaType.APPLICATION_JSON)
                            .body(BodyInserters.fromObject(req))
                            .exchange()
                            .flatMap(clientResponse -> {
                                switch (clientResponse.statusCode()){
                                    case OK:
                                        return clientResponse.bodyToMono(res_get_player_guild.class)
                                                .doOnNext(res->{
                                                    player.guild_id=res.getGuild_id();
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
                .count()
                .block();
    }
    @Test
    //@Ignore
    public void t_000_join()throws Exception{
        String gid="";
        for(int i=0;i<static_data.players.size();++i){
            static_data.player e=static_data.players.get(i);
            if(e.server_name.equals("s003")&&e.player_id.equals("u001"))
                gid=e.guild_id;
        }
        final String guild=gid;
        Flux.fromIterable(static_data.players)
                .filter(player->{
                    //return player.player_id.equals("u002") && player.server_name.equals("s003");
                    return player.player_id.equals("u002") || player.player_id.equals("u003");
                })
                .map(player -> {
                    req_join req=new req_join();
                    req.setGuild_id(guild);
                    if(player.player_id.equals("u002"))
                        req.setLevel(21);
                    else
                        req.setLevel(10);
                    req.setPlayer_id(player.player_id);
                    req.setPlayer_namespace(player.server_name);
                    return req;
                })
                .flatMap(req->{
                    return WebClient.create()
                            .post()
                            .uri("http://10.0.0.11:8080/guild/v1/join")
                            .contentType(MediaType.APPLICATION_JSON)
                            .body(BodyInserters.fromObject(JSON.toJSON(req)))
                            .exchange()
                            .flatMap(clientResponse -> {
                                switch (clientResponse.statusCode()){
                                    case OK:
                                        return clientResponse.bodyToMono(String.class)
                                                .doOnNext(str->{
                                                    logger.info(
                                                            req.getPlayer_namespace()
                                                                    .concat(".")
                                                                    .concat(req.getPlayer_id())
                                                                    .concat(" : ")
                                                                    .concat(str));
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
    @Test
    //@Ignore
    public void t_001_ask()throws Exception{
        String gid1="";
        String gid2="";
        for(int i=0;i<static_data.players.size();++i){
            static_data.player e=static_data.players.get(i);
            if(e.server_name.equals("s001")&&e.player_id.equals("u001"))
                gid1=e.guild_id;
            else if(e.server_name.equals("s002")&&e.player_id.equals("u001"))
                gid2=e.guild_id;
        }

        Flux.just(gid1,gid2)
                .flatMap(guild_id->{
                    return Flux.fromIterable(static_data.players)
                            .filter(player->{
                                return !player.player_id.equals("u001");
                            })
                            .map(player -> {
                                req_ask_join req=new req_ask_join();
                                req.setGuild_id(guild_id);
                                req.setLevel(21);
                                req.setPlayer_id(player.player_id);
                                req.setPlayer_namespace(player.server_name);
                                return req;
                            })
                            .flatMap(req->{
                                //logger.info("try "+req);
                                return WebClient.create()
                                        .post()
                                        .uri("http://10.0.0.11:8080/guild/v1/ask_join")
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .body(BodyInserters.fromObject(JSON.toJSON(req)))
                                        .exchange()
                                        .flatMap(clientResponse -> {
                                            switch (clientResponse.statusCode()){
                                                case OK:
                                                    return clientResponse.bodyToMono(String.class)
                                                            .doOnNext(str->{
                                                                logger.info(str+" "+req);
                                                            })
                                                            .map(str->true);
                                                default:
                                                    return clientResponse
                                                            .bodyToMono(String.class)
                                                            .doOnNext(str->{
                                                                logger.info(str+" "+req);
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
                            });
                })
                .count()
                .block();
    }

    @Test
    //@Ignore
    public void t_002_apply()throws Exception{
        int i1=0,i2=0;
        for(int i=0;i<static_data.players.size();++i){
            static_data.player e=static_data.players.get(i);
            if(e.server_name.equals("s001")&&e.player_id.equals("u001"))
                i1=i;
            else if(e.server_name.equals("s002")&&e.player_id.equals("u001"))
                i2=i;
        }

        Flux.just(i1,i2)
                .flatMap(index->{
                    req_get_ask_list_for_guild req=new req_get_ask_list_for_guild();
                    req.setGuild_id(static_data.players.get(index).guild_id);
                    req.setPlayer_id(static_data.players.get(index).player_id);
                    req.setPlayer_namespace(static_data.players.get(index).server_name);
                    return WebClient.create().post()
                            .uri("http://10.0.0.11:8080/guild/v1/get_guild_ask_list")
                            .contentType(MediaType.APPLICATION_JSON)
                            .body(BodyInserters.fromObject(req))
                            .exchange()
                            .flatMap(clientResponse -> {
                                switch (clientResponse.statusCode()){
                                    case OK:
                                        return clientResponse.bodyToMono(res_get_ask_list_guild.class);
                                    default:
                                        return clientResponse
                                                .bodyToMono(String.class)
                                                .flatMap(str->Mono.error(new Throwable(str)));
                                }
                            })
                            .doOnNext(res->logger.info(static_data.players.get(index).guild_id+" : "+res.toString()))
                            .flatMapMany(res->Flux.fromIterable(res.components))
                            .filter(component -> {
                                switch (static_data.players.get(index).server_name
                                        .concat(".")
                                        .concat(static_data.players.get(index).player_id)){
                                    case "s001.u001":
                                        return component.getPlayer_id().equals("u003");
                                    case "s002.u001":
                                        return component.getPlayer_id().equals("u004");
                                }
                                return false;
                            })
                            //.take(1)
                            .map(component -> {
                                req_apply_join_guild apply_req=new req_apply_join_guild();
                                apply_req.setGuid_id(static_data.players.get(index).guild_id);
                                apply_req.setOwner_id(static_data.players.get(index).player_id);
                                apply_req.setOwner_namespace(static_data.players.get(index).server_name);
                                apply_req.setTarget_id(component.getPlayer_id());
                                apply_req.setTarget_namespace(component.getPlayer_namespace());
                                return  apply_req;
                            });
                })
                .doOnNext(req->logger.info(req.toString()))
                .flatMap(req->{
                    return WebClient.create().post()
                            .uri("http://10.0.0.11:8080/guild/v1/apply_join")
                            .contentType(MediaType.APPLICATION_JSON)
                            .body(BodyInserters.fromObject(req))
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

    @Test
    public void t_003_get_guild_list()throws Exception{
        req_get_guild_list req=new req_get_guild_list();
        req.setName("");
        req.setPlayer_id("u005");
        req.setPlayer_namespace("s001");
        req.setSkip(4);
        req.setTake(4);

        WebClient.create().post()
                .uri("http://10.0.0.11:8080/guild/v1/get_guild_list")
                .contentType(MediaType.APPLICATION_JSON)
                .body(BodyInserters.fromObject(req))
                .exchange()
                .flatMap(clientResponse -> {
                    switch (clientResponse.statusCode()){
                        case OK:
                            return clientResponse.bodyToMono(String.class)
                                    .doOnNext(str->{
                                        logger.info(str);
                                    });
                        default:
                            return clientResponse
                                    .bodyToMono(String.class)
                                    .doOnNext(str->{
                                        logger.error(str);
                                    })
                                    .flatMap(str->Mono.error(new Throwable(str)));
                    }
                })
                .block();
    }

    @Test
    public void t_004_reject()throws Exception{
        int i1=0,i2=0;
        for(int i=0;i<static_data.players.size();++i){
            static_data.player e=static_data.players.get(i);
            if(e.server_name.equals("s001")&&e.player_id.equals("u001"))
                i1=i;
            else if(e.server_name.equals("s002")&&e.player_id.equals("u001"))
                i2=i;
        }

        Flux.just(i1,i2)
                .flatMap(index->{
                    req_get_ask_list_for_guild req=new req_get_ask_list_for_guild();
                    req.setGuild_id(static_data.players.get(index).guild_id);
                    req.setPlayer_id(static_data.players.get(index).player_id);
                    req.setPlayer_namespace(static_data.players.get(index).server_name);
                    return WebClient.create().post()
                            .uri("http://10.0.0.11:8080/guild/v1/get_guild_ask_list")
                            .contentType(MediaType.APPLICATION_JSON)
                            .body(BodyInserters.fromObject(req))
                            .exchange()
                            .flatMap(clientResponse -> {
                                switch (clientResponse.statusCode()){
                                    case OK:
                                        return clientResponse.bodyToMono(res_get_ask_list_guild.class);
                                    default:
                                        return clientResponse
                                                .bodyToMono(String.class)
                                                .flatMap(str->Mono.error(new Throwable(str)));
                                }
                            })
                            .doOnNext(res->logger.info(static_data.players.get(index).guild_id+" : "+res.toString()))
                            .flatMapMany(res->Flux.fromIterable(res.components))
                            //.take(1)
                            .map(component -> {
                                req_reject_join reject_req=new req_reject_join();
                                reject_req.setGuid_id(static_data.players.get(index).guild_id);
                                reject_req.setPlayer_id(static_data.players.get(index).player_id);
                                reject_req.setPlayer_namespace(static_data.players.get(index).server_name);
                                reject_req.setTarget_id(component.getPlayer_id());
                                reject_req.setTarget_namespace(component.getPlayer_namespace());
                                return  reject_req;
                            });
                })
                .doOnNext(req->logger.info(req.toString()))
                .flatMap(req->{
                    return WebClient.create().post()
                            .uri("http://10.0.0.11:8080/guild/v1/reject_join")
                            .contentType(MediaType.APPLICATION_JSON)
                            .body(BodyInserters.fromObject(req))
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

    @Test
    public void t_0051_update_power(){

        String gid="";
        for(int i=0;i<static_data.players.size();++i){
            static_data.player e=static_data.players.get(i);
            if(e.server_name.equals("s001")&&e.player_id.equals("u001"))
                gid=e.guild_id;
        }
        final String guild=gid;

        req_update_power req=new req_update_power();
        req.setComponent(new ArrayList());
        req_update_power_component c1=new req_update_power_component();
        c1.setGuild_id(guild);
        c1.setPlayer_id("u003");
        c1.setPlayer_namespace("s001");
        c1.setPower(2);
        req.getComponent().add(c1);
        req_update_power_component c2=new req_update_power_component();
        c2.setGuild_id(guild);
        c2.setPlayer_id("u003");
        c2.setPlayer_namespace("s002");
        c2.setPower(1);
        req.getComponent().add(c2);



        WebClient.create().post()
                .uri("http://10.0.0.11:8080/guild/v1/update_power")
                .contentType(MediaType.APPLICATION_JSON)
                .body(BodyInserters.fromObject(req))
                .exchange()
                .flatMap(clientResponse -> {
                    switch (clientResponse.statusCode()){
                        case OK:
                            return clientResponse.bodyToMono(String.class)
                                    .doOnNext(str->{
                                        logger.info(str);
                                    });
                        default:
                            return clientResponse
                                    .bodyToMono(String.class)
                                    .doOnNext(str->{
                                        logger.error(str);
                                    })
                                    .flatMap(str->Mono.error(new Throwable(str)));
                    }
                })
                .block();
    }

    @Test
    public void t_005_get_guild_info(){

        String gid="";
        for(int i=0;i<static_data.players.size();++i){
            static_data.player e=static_data.players.get(i);
            if(e.server_name.equals("s001")&&e.player_id.equals("u001"))
                gid=e.guild_id;
        }
        final String guild=gid;

        req_get_guild_info req=new req_get_guild_info();
        req.setGuild_id(guild);
        req.setPlayer_id("u003");
        req.setPlayer_namespace("s002");

        WebClient.create().post()
                .uri("http://10.0.0.11:8080/guild/v1/get_guild_info")
                .contentType(MediaType.APPLICATION_JSON)
                .body(BodyInserters.fromObject(req))
                .exchange()
                .flatMap(clientResponse -> {
                    switch (clientResponse.statusCode()){
                        case OK:
                            return clientResponse.bodyToMono(String.class)
                                    .doOnNext(str->{
                                        logger.info(str);
                                    });
                        default:
                            return clientResponse
                                    .bodyToMono(String.class)
                                    .doOnNext(str->{
                                        logger.error(str);
                                    })
                                    .flatMap(str->Mono.error(new Throwable(str)));
                    }
                })
                .block();
    }

    @Test
    public void t_006_get_members(){

        String gid="";
        for(int i=0;i<static_data.players.size();++i){
            static_data.player e=static_data.players.get(i);
            if(e.server_name.equals("s001")&&e.player_id.equals("u001"))
                gid=e.guild_id;
        }
        final String guild=gid;

        req_get_members req=new req_get_members();
        req.setGuild_id(guild);
        req.setPlayer_id("u005");
        req.setPlayer_namespace("s001");

        WebClient.create().post()
                .uri("http://10.0.0.11:8080/guild/v1/get_members")
                .contentType(MediaType.APPLICATION_JSON)
                .body(BodyInserters.fromObject(req))
                .exchange()
                .flatMap(clientResponse -> {
                    switch (clientResponse.statusCode()){
                        case OK:
                            return clientResponse.bodyToMono(String.class)
                                    .doOnNext(str->{
                                        logger.info(str);
                                    });
                        default:
                            return clientResponse
                                    .bodyToMono(String.class)
                                    .doOnNext(str->{
                                        logger.error(str);
                                    })
                                    .flatMap(str->Mono.error(new Throwable(str)));
                    }
                })
                .block();
    }

    @Test
    public void t_007_set_pos(){

        String gid="";
        for(int i=0;i<static_data.players.size();++i){
            static_data.player e=static_data.players.get(i);
            if(e.server_name.equals("s001")&&e.player_id.equals("u001"))
                gid=e.guild_id;
        }
        final String guild=gid;

        req_set_pos req=new req_set_pos();
        req.setGuid_id(guild);
        req.setPlayer_id("u001");
        req.setPlayer_namespace("s001");
        req.setTarget_id("u003");
        req.setTarget_namespace("s002");
        req.setPos(2);

        WebClient.create().post()
                .uri("http://10.0.0.11:8080/guild/v1/set_pos")
                .contentType(MediaType.APPLICATION_JSON)
                .body(BodyInserters.fromObject(req))
                .exchange()
                .flatMap(clientResponse -> {
                    switch (clientResponse.statusCode()){
                        case OK:
                            return clientResponse.bodyToMono(String.class)
                                    .doOnNext(str->{
                                        logger.info(str);
                                    });
                        default:
                            return clientResponse
                                    .bodyToMono(String.class)
                                    .doOnNext(str->{
                                        logger.error(str);
                                    })
                                    .flatMap(str->Mono.error(new Throwable(str)));
                    }
                })
                .block();
    }

    @Test
    public void t_008_get_member_detail(){

        String gid="";
        for(int i=0;i<static_data.players.size();++i){
            static_data.player e=static_data.players.get(i);
            if(e.server_name.equals("s001")&&e.player_id.equals("u001"))
                gid=e.guild_id;
        }
        final String guild=gid;

        req_get_member_detail req=new req_get_member_detail();
        req.setGuild_id(guild);
        req.setPlayer_id("u003");
        req.setPlayer_namespace("s002");
        req.setDonate_log_reset_expiration(System.currentTimeMillis()+24*3600*1000);
        req.setDonate_experience_reset_expiration(System.currentTimeMillis()+24*3600*1000);

        WebClient.create().post()
                .uri("http://10.0.0.11:8080/guild/v1/get_member_detail")
                .contentType(MediaType.APPLICATION_JSON)
                .body(BodyInserters.fromObject(req))
                .exchange()
                .flatMap(clientResponse -> {
                    switch (clientResponse.statusCode()){
                        case OK:
                            return clientResponse.bodyToMono(String.class)
                                    .doOnNext(str->{
                                        logger.info(str);
                                    });
                        default:
                            return clientResponse
                                    .bodyToMono(String.class)
                                    .doOnNext(str->{
                                        logger.error(str);
                                    })
                                    .flatMap(str->Mono.error(new Throwable(str)));
                    }
                })
                .block();
    }

    @Test
    public void t_008_donate(){

        String gid="";
        for(int i=0;i<static_data.players.size();++i){
            static_data.player e=static_data.players.get(i);
            if(e.server_name.equals("s001")&&e.player_id.equals("u001"))
                gid=e.guild_id;
        }
        final String guild=gid;

        req_donate req=new req_donate();
        req.setGuid_id(guild);
        req.setPlayer_id("u003");
        req.setPlayer_namespace("s002");
        req.setDonate_id("0105");
        req.setDonate_reset_expiration(System.currentTimeMillis()+3600*1000);
        req.setExperience(100);
        req.setExperience_reset_expiration(System.currentTimeMillis()+3600*1000);
        req.setMx_donate_count(15);

        WebClient.create().post()
                .uri("http://10.0.0.11:8080/guild/v1/donate")
                .contentType(MediaType.APPLICATION_JSON)
                .body(BodyInserters.fromObject(req))
                .exchange()
                .flatMap(clientResponse -> {
                    switch (clientResponse.statusCode()){
                        case OK:
                            return clientResponse.bodyToMono(String.class)
                                    .doOnNext(str->{
                                        logger.info(str);
                                    });
                        default:
                            return clientResponse
                                    .bodyToMono(String.class)
                                    .doOnNext(str->{
                                        logger.error(str);
                                    })
                                    .flatMap(str->Mono.error(new Throwable(str)));
                    }
                })
                .block();
    }

    @Test
    public void t_009_quit(){

        String gid="";
        for(int i=0;i<static_data.players.size();++i){
            static_data.player e=static_data.players.get(i);
            if(e.server_name.equals("s001")&&e.player_id.equals("u001"))
                gid=e.guild_id;
        }
        final String guild=gid;

        req_quit_guild req=new req_quit_guild();
        req.setGuild_id(guild);
        req.setPlayer_id("u003");
        req.setPlayer_namespace("s001");

        WebClient.create().post()
                .uri("http://10.0.0.11:8080/guild/v1/quit_guild")
                .contentType(MediaType.APPLICATION_JSON)
                .body(BodyInserters.fromObject(req))
                .exchange()
                .flatMap(clientResponse -> {
                    switch (clientResponse.statusCode()){
                        case OK:
                            return clientResponse.bodyToMono(String.class)
                                    .doOnNext(str->{
                                        logger.info(str);
                                    });
                        default:
                            return clientResponse
                                    .bodyToMono(String.class)
                                    .doOnNext(str->{
                                        logger.error(str);
                                    })
                                    .flatMap(str->Mono.error(new Throwable(str)));
                    }
                })
                .block();
    }
    @Test
    public void t_010_remove(){

        String gid="";
        for(int i=0;i<static_data.players.size();++i){
            static_data.player e=static_data.players.get(i);
            if(e.server_name.equals("s001")&&e.player_id.equals("u001"))
                gid=e.guild_id;
        }
        final String guild=gid;

        req_remove_member req=new req_remove_member();
        req.setGuid_id(guild);
        req.setPlayer_id("u001");
        req.setPlayer_namespace("s001");
        req.setTarget_namespace("s003");
        req.setTarget_id("u003");

        WebClient.create().post()
                .uri("http://10.0.0.11:8080/guild/v1/remove_member")
                .contentType(MediaType.APPLICATION_JSON)
                .body(BodyInserters.fromObject(req))
                .exchange()
                .flatMap(clientResponse -> {
                    switch (clientResponse.statusCode()){
                        case OK:
                            return clientResponse.bodyToMono(String.class)
                                    .doOnNext(str->{
                                        logger.info(str);
                                    });
                        default:
                            return clientResponse
                                    .bodyToMono(String.class)
                                    .doOnNext(str->{
                                        logger.error(str);
                                    })
                                    .flatMap(str->Mono.error(new Throwable(str)));
                    }
                })
                .block();
    }

    @Test
    public void t_011_change_owner(){

        String gid="";
        for(int i=0;i<static_data.players.size();++i){
            static_data.player e=static_data.players.get(i);
            if(e.server_name.equals("s002")&&e.player_id.equals("u001"))
                gid=e.guild_id;
        }
        final String guild=gid;

        req_change_owner req=new req_change_owner();
        req.setGuid_id(guild);
        req.setPlayer_namespace("s002");
        req.setPlayer_id("u001");
        req.setTarget_namespace("s001");
        req.setTarget_id("u004");

        WebClient.create().post()
                .uri("http://10.0.0.11:8080/guild/v1/change_owner")
                .contentType(MediaType.APPLICATION_JSON)
                .body(BodyInserters.fromObject(req))
                .exchange()
                .flatMap(clientResponse -> {
                    switch (clientResponse.statusCode()){
                        case OK:
                            return clientResponse.bodyToMono(String.class)
                                    .doOnNext(str->{
                                        logger.info(str);
                                    });
                        default:
                            return clientResponse
                                    .bodyToMono(String.class)
                                    .doOnNext(str->{
                                        logger.error(str);
                                    })
                                    .flatMap(str->Mono.error(new Throwable(str)));
                    }
                })
                .block();
    }
    @Test
    public void t_012_test_issue_help(){

        String gid="";
        for(int i=0;i<static_data.players.size();++i){
            static_data.player e=static_data.players.get(i);
            if(e.server_name.equals("s003")&&e.player_id.equals("u001"))
                gid=e.guild_id;
        }
        final String guild=gid;

        req_issue_help req=new req_issue_help();
        req.setGuild_id(guild);
        req.setHelp_id("0208");
        req.setHelp_level(12);
        req.setMax_help_count(15);
        req.setMax_coin_count(15);
        req.setPlayer_id("u002");
        req.setPlayer_namespace("s002");

        WebClient.create().post()
                .uri("http://10.0.0.11:8080/guild/v1/issue_help")
                .contentType(MediaType.APPLICATION_JSON)
                .body(BodyInserters.fromObject(req))
                .exchange()
                .flatMap(clientResponse -> {
                    switch (clientResponse.statusCode()){
                        case OK:
                            return clientResponse.bodyToMono(String.class)
                                    .doOnNext(str->{
                                        logger.info(str);
                                    });
                        default:
                            return clientResponse
                                    .bodyToMono(String.class)
                                    .doOnNext(str->{
                                        logger.error(str);
                                    })
                                    .flatMap(str->Mono.error(new Throwable(str)));
                    }
                })
                .block();
    }

    @Test
    public void t_012_x_test_do_help(){

        String gid="";
        for(int i=0;i<static_data.players.size();++i){
            static_data.player e=static_data.players.get(i);
            if(e.server_name.equals("s003")&&e.player_id.equals("u001"))
                gid=e.guild_id;
        }
        final String guild=gid;

        req_do_guild_help req=new req_do_guild_help();
        req.setGuild_id(guild);
        req.setTarget_help_id("0208");
        req.setTarget_help_level(12);
        req.setPlayer_id("u001");
        req.setPlayer_namespace("s003");
        req.setTarget_id("u002");
        req.setTarget_namespace("s002");

        WebClient.create().post()
                .uri("http://10.0.0.11:8080/guild/v1/do_guild_help")
                .contentType(MediaType.APPLICATION_JSON)
                .body(BodyInserters.fromObject(req))
                .exchange()
                .flatMap(clientResponse -> {
                    switch (clientResponse.statusCode()){
                        case OK:
                            return clientResponse.bodyToMono(String.class)
                                    .doOnNext(str->{
                                        logger.info(str);
                                    });
                        default:
                            return clientResponse
                                    .bodyToMono(String.class)
                                    .doOnNext(str->{
                                        logger.error(str);
                                    })
                                    .flatMap(str->Mono.error(new Throwable(str)));
                    }
                })
                .block();
    }
    @Test
    public void t_012_y_test_get_help(){

        String gid="";
        for(int i=0;i<static_data.players.size();++i){
            static_data.player e=static_data.players.get(i);
            if(e.server_name.equals("s003")&&e.player_id.equals("u001"))
                gid=e.guild_id;
        }
        final String guild=gid;

        req_get_help_list req=new req_get_help_list();
        req.setGuild_id(guild);
        req.setPlayer_id("u002");
        req.setPlayer_namespace("s002");

        WebClient.create().post()
                .uri("http://10.0.0.11:8080/guild/v1/get_guild_help_list")
                .contentType(MediaType.APPLICATION_JSON)
                .body(BodyInserters.fromObject(req))
                .exchange()
                .flatMap(clientResponse -> {
                    switch (clientResponse.statusCode()){
                        case OK:
                            return clientResponse.bodyToMono(String.class)
                                    .doOnNext(str->{
                                        logger.info(str);
                                    });
                        default:
                            return clientResponse
                                    .bodyToMono(String.class)
                                    .doOnNext(str->{
                                        logger.error(str);
                                    })
                                    .flatMap(str->Mono.error(new Throwable(str)));
                    }
                })
                .block();
    }

    @Test
    public void t_013_test_drop_guild(){

        String gid="";
        for(int i=0;i<static_data.players.size();++i){
            static_data.player e=static_data.players.get(i);
            if(e.server_name.equals("s005")&&e.player_id.equals("u001"))
                gid=e.guild_id;
        }
        final String guild=gid;

        req_drop_guild req=new req_drop_guild();
        req.setGuild_id(guild);
        req.setPlayer_id("u001");
        req.setPlayer_namespace("s005");

        WebClient.create().post()
                .uri("http://10.0.0.11:8080/guild/v1/drop_guild")
                .contentType(MediaType.APPLICATION_JSON)
                .body(BodyInserters.fromObject(req))
                .exchange()
                .flatMap(clientResponse -> {
                    switch (clientResponse.statusCode()){
                        case OK:
                            return clientResponse.bodyToMono(String.class)
                                    .doOnNext(str->{
                                        logger.info(str);
                                    });
                        default:
                            return clientResponse
                                    .bodyToMono(String.class)
                                    .doOnNext(str->{
                                        logger.error(str);
                                    })
                                    .flatMap(str->Mono.error(new Throwable(str)));
                    }
                })
                .block();

    }

    @Test
    public void t_014_test_recreate_new(){
        Flux.fromIterable(static_data.players)
                .filter(player->{
                    return player.player_id.equals("u001") &&player.server_name.equals("s005");
                })
                .map(player->{
                    req_new_guild req=new req_new_guild();
                    req.setDomain_id("10");
                    req.setDeclaration("1111");
                    req.setFlag("1");
                    req.setLevel_for_join(20);
                    req.setMax_member_count(20);
                    //req.setName(player.server_name.concat(".").concat(player.player_id).concat(".new"));
                    req.setName("唱儿歌大大");
                    req.setLanguage("CN");
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

    @Test
    public void t_015_test_guild_mail(){
        @Data
        class mail_attachment{
            String id;
            String content;
            mail_attachment(String id,String content){
                this.id=id;this.content=content;
            }
        }

        String gid="";
        for(int i=0;i<static_data.players.size();++i){
            static_data.player e=static_data.players.get(i);
            if(e.server_name.equals("s002")&&e.player_id.equals("u001"))
                gid=e.guild_id;
        }
        final String guild=gid;

        String[] attachment=new String[2];
        attachment[0]=(JSON.toJSONString(new mail_attachment("001","AAA")));
        attachment[1]=(JSON.toJSONString(new mail_attachment("002","BBB")));

        req_new_guild_mail req=new req_new_guild_mail();
        req.setAttechmentJson(attachment);
        req.setCondition("");
        req.setFrom("t_015_test_guild_mail");
        req.setTitle("a mail");
        req.setType("A");
        req.setContent("mail");
        req.setPlayer_id("u004");
        req.setPlayer_namespace("s001");
        req.setGuild_id(guild);

        WebClient.create()
                .post()
                .uri("http://10.0.0.11:8080/guild/v1/send_guild_mail")
                .contentType(MediaType.APPLICATION_JSON)
                .body(BodyInserters.fromObject(JSON.toJSON(req)))
                .exchange()
                .flatMap(clientResponse -> {
                    switch (clientResponse.statusCode()){
                        case OK:
                            return clientResponse.bodyToMono(String.class)
                                    .doOnNext(str->{
                                        logger.info(str);
                                    });
                        default:
                            return clientResponse
                                    .bodyToMono(String.class)
                                    .doOnNext(str->{
                                        logger.error(str);
                                    })
                                    .flatMap(str->Mono.error(new Throwable(str)));
                    }
                })
        .block();
    }

    @Test
    public void t_016_quit(){

        String gid="";
        for(int i=0;i<static_data.players.size();++i){
            static_data.player e=static_data.players.get(i);
            if(e.server_name.equals("s003")&&e.player_id.equals("u001"))
                gid=e.guild_id;
        }
        final String guild=gid;

        req_quit_guild req=new req_quit_guild();
        req.setGuild_id(guild);
        req.setPlayer_id("u002");
        req.setPlayer_namespace("s002");

        WebClient.create().post()
                .uri("http://10.0.0.11:8080/guild/v1/quit_guild")
                .contentType(MediaType.APPLICATION_JSON)
                .body(BodyInserters.fromObject(req))
                .exchange()
                .flatMap(clientResponse -> {
                    switch (clientResponse.statusCode()){
                        case OK:
                            return clientResponse.bodyToMono(String.class)
                                    .doOnNext(str->{
                                        logger.info(str);
                                    });
                        default:
                            return clientResponse
                                    .bodyToMono(String.class)
                                    .doOnNext(str->{
                                        logger.error(str);
                                    })
                                    .flatMap(str->Mono.error(new Throwable(str)));
                    }
                })
                .block();
    }

    @Test
    public void t_017_get_guild_info_again(){

        String gid="";
        for(int i=0;i<static_data.players.size();++i){
            static_data.player e=static_data.players.get(i);
            if(e.server_name.equals("s003")&&e.player_id.equals("u001"))
                gid=e.guild_id;
        }
        final String guild=gid;

        req_get_guild_info req=new req_get_guild_info();
        req.setGuild_id(guild);
        req.setPlayer_id("u002");
        req.setPlayer_namespace("s002");

        WebClient.create().post()
                .uri("http://10.0.0.11:8080/guild/v1/get_guild_info")
                .contentType(MediaType.APPLICATION_JSON)
                .body(BodyInserters.fromObject(req))
                .exchange()
                .flatMap(clientResponse -> {
                    switch (clientResponse.statusCode()){
                        case OK:
                            return clientResponse.bodyToMono(String.class)
                                    .doOnNext(str->{
                                        logger.info(str);
                                    });
                        default:
                            return clientResponse
                                    .bodyToMono(String.class)
                                    .doOnNext(str->{
                                        logger.error(str);
                                    })
                                    .flatMap(str->Mono.error(new Throwable(str)));
                    }
                })
                .block();
    }
}
