package com.cyworld.social.data.entity.guild;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.cyworld.social.data.entity.base.i_deep_clone;
import com.cyworld.social.utils.request.guild.*;
import com.cyworld.social.utils.response.guild.*;
import lombok.Builder;
import org.apache.logging.log4j.util.Strings;
import org.springframework.security.crypto.codec.Hex;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Builder
public class guild implements i_deep_clone<guild> {
    public String id;
    public String name;
    public String domain_id;
    @Builder.Default
    public String language="CN";
    @Builder.Default
    public boolean opening=true;
    @Builder.Default
    public String declaration= Strings.EMPTY;
    @Builder.Default
    public String notice= Strings.EMPTY;
    public String flag;
    public boolean need_apply_when_join;
    public int level_for_join;
    //public guild_member owner;
    public long power;
    public long exp;
    public int max_member_count;
    @Builder.Default
    public List<String> log=new ArrayList<>();
    @Builder.Default
    public List<guild_member> members=new ArrayList<>();
    @Builder.Default
    public List<guild_member> quit_members=new ArrayList<>();
    @Builder.Default
    public List<guild_ask_info_guild> ask_list=new ArrayList<>();
    @Builder.Default
    public guild_help_info help_info=new guild_help_info();

    @Override
    public guild deep_clone() {
        return guild.builder()
                .power(this.power)
                .id(this.id)
                .name(this.name)
                .opening(this.opening)
                .domain_id(this.domain_id)
                .language(this.language)
                .declaration(this.declaration)
                .notice(this.notice)
                .flag(this.flag)
                .need_apply_when_join(this.need_apply_when_join)
                .level_for_join(this.level_for_join)
                .power(this.power)
                .exp(this.exp)
                .max_member_count(this.max_member_count)
                .log(this.log.stream().collect(Collectors.toList()))
                .members(this.members.stream().map(e->e.deep_clone()).collect(Collectors.toList()))
                .quit_members(this.quit_members.stream().map(e->e.deep_clone()).collect(Collectors.toList()))
                .ask_list(this.ask_list.stream().map(e->e.deep_clone()).collect(Collectors.toList()))
                .help_info(this.help_info.deep_clone())
                .build();
    }

    public void clear_expiration_quit_members(){
        long now=System.currentTimeMillis();
        quit_members=quit_members.stream().filter(o->o.getReg_timer()>now).collect(Collectors.toList());
    }
    public void clear_expiration_ask_list(){
        long now=System.currentTimeMillis();
        ask_list=ask_list.stream().filter(o->o.getExpiration()>now).collect(Collectors.toList());
    }

    public guild_member find_member(String namespace,String id){
        for(int i=0;i<members.size();++i){
            guild_member info=members.get(i);
            if(info.getPlayer_id().toUpperCase().equals(id.toUpperCase())
                    && info.getPlayer_namespace().toUpperCase().equals(namespace.toUpperCase())
            )
                return info;
        }
        return null;
    }

    public boolean add_coin(String namespace,String id,long expiration_timer,long max_coin){
        guild_member src=find_member(namespace,id);
        if(src==null)
            return false;
        src.getCoin().try_reset(expiration_timer);
        if(src.getCoin().target.coin>max_coin)
            return false;
        src.getCoin().target.coin++;
        return true;
    }

    public int remove_member(req_remove_member req){
        guild_member src=find_member(req.getPlayer_namespace(),req.getPlayer_id());
        guild_member tar=find_member(req.getTarget_namespace(),req.getTarget_id());
        if(src==null || tar==null)
            return res_remove_member.result_not_member;
        if(src.getPosition()<=tar.getPosition())
            return res_remove_member.result_need_pos;
        for(int i=0;i<members.size();++i){
            guild_member info=members.get(i);
            if(info.getPlayer_id().equals(req.getTarget_id())
                    && info.getPlayer_namespace().equals(req.getTarget_namespace())
            ){
                members.remove(i);
                on_member_quited(info);
                return res_remove_member.result_ok;
            }
        }
        return res_remove_member.result_not_member;
    }
    private void change_info(String k,String v){
        switch (k){
            case "declaration":
                this.declaration=v;
                return;
            case "notice":
                this.notice=v;
                return;
            case "need_apply_when_join":
                this.need_apply_when_join=Boolean.valueOf(v);
                return;
            case "level_for_join":
                this.level_for_join=Integer.valueOf(v);
                return;
            case "flag":
                this.flag=v;
                return;
        }
    }
    public void add_log(req_add_log req){
        this.log.add(req.getLog());
        while (this.log.size()>=req.getMax_log_count())
            this.log.remove(0);
    }
    public int change_info(req_change_guild_info req){
        guild_member src_member=find_member(req.getPlayer_namespace(),req.getPlayer_id());
        if(src_member==null)
            return res_change_guild_info.result_err_need_pos;
        if(src_member.getPosition()!=4)
            return res_change_guild_info.result_err_need_pos;
        JSONObject jo= JSON.parseObject(req.getJson_value());
        jo.keySet().stream().forEach(e->{
            change_info(e,jo.getString(e));
        });
        return res_change_guild_info.result_ok;
    }
    public int change_owner(req_change_owner req){
        guild_member src=find_member(req.getPlayer_namespace(),req.getPlayer_id());
        guild_member tar=find_member(req.getTarget_namespace(),req.getTarget_id());
        if(src==null)
            return res_change_owner.result_err_not_member;
        if(tar==null)
            return res_change_owner.result_err_not_member;
        if(src.getPosition()!=4)
            return res_change_owner.result_err_not_owner;
        src.setPosition(tar.position);
        tar.setPosition(4);

        return res_change_owner.result_ok;
    }

    public int get_member_pos(String player_namespace,String player_id){
        guild_member m= find_member(player_namespace,player_id);
        if(m!=null)
            return m.getPosition();
        else
            return -1;
    }
    public int set_member_pos(req_set_pos req,int max_vp_count,int max_officer_count){
        guild_member src=find_member(req.getPlayer_namespace(),req.getPlayer_id());
        guild_member tar=find_member(req.getTarget_namespace(),req.getTarget_id());
        if(src==null || tar==null)
            return res_set_pos.result_err_not_member;
        if(src.getPosition()<req.getPos())
            return res_set_pos.result_err_need_pos;
        if(src.getPosition()<=tar.getPosition())
            return res_set_pos.result_err_need_pos;

        if(req.getPos()==3){
            if(this.members.stream().filter(e->e.getPosition()==3).count()>=max_vp_count)
                return res_set_pos.result_max_vp_count;
        }
        if(req.getPos()==2){
            if(this.members.stream().filter(e->e.getPosition()==2).count()>=max_officer_count)
                return res_set_pos.result_max_vp_count;
        }
        tar.setPosition(req.getPos());
        return res_set_pos.result_ok;
    }
    private void on_member_quited(guild_member who){
        who.setReg_timer(System.currentTimeMillis()+24*3600*1000);
        help_info.del_guild_help(who);
        quit_members.add(who);
        this.power-=who.power;
    }
    public void clear_all_member_info(String player_namespace,String player_id){
        help_info.del_guild_help(player_namespace,player_id);
    }
    public int quit(req_quit_guild req){
        for(int i=0;i<members.size();++i){
            guild_member info=members.get(i);
            if(info.getPlayer_id().equals(req.getPlayer_id())
                    && info.getPlayer_namespace().equals(req.getPlayer_namespace())
            ){
                if(info.getPosition()==4){
                    return res_quit_guild.result_you_are_owner;
                }
                else{
                    boolean quited=false;
                    try {
                        members.remove(i);
                        quited=true;
                        on_member_quited(info);
                        return res_quit_guild.result_ok;
                    }
                    catch (Exception e){
                        if(quited)
                            members.add(info);
                        return res_quit_guild.result_unk;
                    }
                }

            }
        }
        return res_quit_guild.result_ok;
    }

    public boolean reject_join(req_reject_join req){
        guild_member src=find_member(req.getPlayer_namespace(),req.getPlayer_id());
        if(src==null)
            return false;
        if(req.isReject_all()){
            if(src.getPosition()==4)
                return false;
            this.ask_list.clear();
            return true;
        }
        else {
            if(src.getPosition()<2)
                return false;
            for(int i=0;i<ask_list.size();++i){
                guild_ask_info_guild info=ask_list.get(i);
                if(info.getSrc_namespace().equals(req.getTarget_namespace())
                        && info.getSrc_id().equals(req.getTarget_id())
                ){
                    ask_list.remove(i);
                    return true;
                }
            }
            return false;
        }
    }

    public int apply_join(req_apply_join_guild req){
        if (!opening)
            return res_join.result_max_count;
        guild_member src=find_member(req.getOwner_namespace(),req.getOwner_id());
        if(src==null)
            return res_apply_join.result_not_owner;
        if(src.getPosition()<4)
            return res_apply_join.result_not_owner;
        for(int i=0;i<ask_list.size();++i){
            guild_ask_info_guild info=ask_list.get(i);
            if(info.getSrc_namespace().equals(req.getTarget_namespace())
                    && info.getSrc_id().equals(req.getTarget_id())
            ){
                ask_list.remove(i);
                members.add(guild_member
                        .builder()
                        .guild_id(id)
                        .position(1)
                        .power(info.power)
                        .reg_timer(System.currentTimeMillis())
                        .player_id(info.getSrc_id())
                        .player_namespace(info.getSrc_namespace())
                        .build());
                this.power+=info.power;
                return res_apply_join.result_ok;
            }
        }
        return res_apply_join.result_not_asked;
    }

    public int join(req_join req){
        if (!opening)
            return res_join.result_max_count;
        if (max_member_count<=members.size()){
            return res_join.result_max_count;
        }

        if (level_for_join>req.getLevel()){
            return res_join.result_level_mismatching;
        }

        if (members.stream().anyMatch(o->{
            return o.getPlayer_id().equals(req.getPlayer_id()) && o.getPlayer_namespace().equals(req.getPlayer_namespace());
        })){
            return res_join.result_has_joined;
        }

        clear_expiration_quit_members();

        if (quit_members.stream().anyMatch(o->{
            return o.getPlayer_id().equals(req.getPlayer_id()) && o.getPlayer_namespace().equals(req.getPlayer_namespace());
        })){
            return res_join.result_quit_in_24;
        }

        if (need_apply_when_join){
            return res_join.result_has_asked;
        }

        members.add(guild_member
                .builder()
                .player_namespace(req.getPlayer_namespace())
                .player_id(req.getPlayer_id())
                .power(req.getPower())
                .position(1)
                .reg_timer(System.currentTimeMillis())
                .build());
        this.power+=req.getPower();
        return res_ask_join_guild.result_ok;

    }

    public int ask_join(req_ask_join req){
        if (!opening)
            return res_join.result_max_count;
        if (max_member_count<=members.size()){
            return res_ask_join_guild.result_max_count;
        }

        if (level_for_join>req.getLevel()){
            return res_ask_join_guild.result_level_mismatching;
        }

        if (members.stream().anyMatch(o->{
            return o.getPlayer_id().equals(req.getPlayer_id()) && o.getPlayer_namespace().equals(req.getPlayer_namespace());
        })){
            return res_ask_join_guild.result_has_joined;
        }

        clear_expiration_quit_members();

        if (quit_members.stream().anyMatch(o->{
            return o.getPlayer_id().equals(req.getPlayer_id()) && o.getPlayer_namespace().equals(req.getPlayer_namespace());
        })){
            return res_ask_join_guild.result_quit_in_24;
        }

        if(!need_apply_when_join)
            return res_ask_join_guild.result_needent;

        ask_list.add(guild_ask_info_guild
                .builder()
                .src_namespace(req.getPlayer_namespace())
                .expiration(24*3600*1000+System.currentTimeMillis())
                .power(req.getPower())
                .src_id(req.getPlayer_id())
                .build());
        return res_ask_join_guild.result_ok;
    }

    public boolean is_owner(String namespace,String id){
        guild_member src=find_member(namespace,id);
        if(src==null)
            return false;
        if(src.getPosition()<4)
            return false;
        return true;
    }
    public guild_member find_owner(){
        for(int i=0;i<members.size();++i){
            guild_member info=members.get(i);
            if(info.getPosition()==4)
                return info;
        }
        return null;
    }

    public void update_members_power(List<req_update_power_component> lst){
        lst.stream().forEach(e->{
            guild_member m=find_member(e.getPlayer_namespace(),e.getPlayer_id());
            if(m==null)
                return;
            this.power-=m.power;
            this.power+=e.getPower();
            m.power=e.getPower();
        });
    }
    public void update_member_donate(guild_member m,req_get_member_detail req){
        if(m==null)
            return;
        m.donate_experience.try_reset(req.getDonate_experience_reset_expiration());
        m.donate_log.try_reset(req.getDonate_log_reset_expiration());
    }
    public int update_member_donate(req_donate req){
        guild_member m=find_member(req.getPlayer_namespace(),req.getPlayer_id());
        if(m==null)
            return res_donate.result_err_guild_id;

        m.donate_experience.try_reset(req.getExperience_reset_expiration());
        m.donate_log.try_reset(req.getDonate_reset_expiration());


        Long count=m.donate_log.target.donate.getLong(req.getDonate_id());
        if(count==null)
            count=Long.valueOf(0);
        if(count>=req.getMx_donate_count())
            return res_donate.result_max_donate;
        count++;
        m.donate_log.target.donate.put(req.getDonate_id(),count);
        m.donate_experience.target.exp+=req.getExperience();
        this.exp+=req.getExperience();
        return res_donate.result_ok;
    }
    public void fill_res_get_guild_info(res_get_guild_info res,String player_namespace,String player_id){
        guild_member owner=this.find_owner();
        res.setDeclaration(this.declaration);
        res.setExp(this.exp);
        res.setFlag(this.flag);
        res.setLanguage(this.language);
        res.setId(this.id);
        res.setMax_member_count(this.max_member_count);
        res.setMember_count(this.members.size());
        res.setName(this.name);
        res.setNotice(this.notice);
        res.setNeed_apply(this.need_apply_when_join);
        res.setPlayer_level_for_join(this.level_for_join);
        res.setOwner_id(owner.getPlayer_id());
        res.setOwner_namespace(owner.getPlayer_namespace());
        res.setPower(this.power);
        res.setLog(JSON.toJSONString(this.log));
        res.setPos(this.get_member_pos(player_namespace,player_id));
    }

    public static guild from_request(req_new_guild req){
        guild_member owner=guild_member.builder()
                .player_id(req.getPlayer_id())
                .player_namespace(req.getPlayer_namespace())
                .position(4)
                .reg_timer(System.currentTimeMillis())
                .power(req.getOwner_power())
                .guild_id(req.getName())
                .build();
        guild new_guild= guild.builder()
                //.id(req.getName())
                .id(String.valueOf(Hex.encode(req.getName().getBytes())))
                .name(req.getName())
                .language(req.getLanguage())
                .domain_id(req.getDomain_id())
                .declaration(req.getDeclaration())
                .notice(req.getNotice())
                .flag(req.getFlag())
                .need_apply_when_join(req.isNeed_apply_when_join())
                .level_for_join(req.getLevel_for_join())
                .power(owner.power)
                .max_member_count(req.getMax_member_count())
                .log(new ArrayList<>())
                .members(new ArrayList<>())
                .quit_members(new ArrayList<>())
                .ask_list(new ArrayList<>())
                .build();
        new_guild.members.add(owner);
        return new_guild;
    }
}
