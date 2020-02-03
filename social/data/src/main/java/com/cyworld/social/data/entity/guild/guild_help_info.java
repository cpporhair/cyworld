package com.cyworld.social.data.entity.guild;

import com.cyworld.social.data.entity.base.i_deep_clone;
import com.cyworld.social.utils.request.guild.req_del_guild_help;
import com.cyworld.social.utils.request.guild.req_do_guild_help;
import com.cyworld.social.utils.request.guild.req_get_help_list;
import com.cyworld.social.utils.request.guild.req_issue_help;
import com.cyworld.social.utils.response.guild.res_do_guild_help;
import com.cyworld.social.utils.response.guild.res_get_guild_help_component;
import com.cyworld.social.utils.response.guild.res_get_guild_help_list;
import com.cyworld.social.utils.response.guild.res_issue_help;
import lombok.Builder;
import lombok.Data;
import reactor.util.function.Tuple2;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class guild_help_info implements i_deep_clone<guild_help_info> {
    @Data
    public static class guild_help_data{
        String src_id;
        String src_namespace;
        String help_type_id;
        int cur_level;
        int max_help_count;
        int max_coin_count;
        List<String> help_members=new ArrayList<>();
    }

    public Map<String,guild_help_data> datas=new HashMap<>();

    //public List<guild_help_data> datas=new ArrayList<>();

    @Override
    public guild_help_info deep_clone() {
        guild_help_info new_info=new guild_help_info();
        this.datas.entrySet().stream().forEach(e->{
            guild_help_data new_data=new guild_help_data();
            new_data.setHelp_type_id(e.getValue().help_type_id);
            new_data.setMax_help_count(e.getValue().max_help_count);
            new_data.setSrc_id(e.getValue().src_id);
            new_data.setSrc_namespace(e.getValue().src_namespace);
            new_data.help_members.addAll(e.getValue().help_members);
            new_data.setCur_level(e.getValue().cur_level);
            new_data.setMax_coin_count(e.getValue().max_coin_count);
            new_info.datas.put(e.getKey(),new_data);
        });
        return new_info;
    }

    public int get_max_coin_count(String namespace,String id,String hid){
        String this_help_key=namespace
                .concat("_")
                .concat(id)
                .concat("_")
                .concat(hid);
        guild_help_data data=datas.get(this_help_key);
        if(data==null)
            return 0;
        return data.getMax_coin_count();
    }

    public int issue_help(req_issue_help req){
        String this_help_key=req.getPlayer_namespace()
                .concat("_")
                .concat(req.getPlayer_id())
                .concat("_")
                .concat(req.getHelp_id());
        guild_help_data old_data=datas.get(this_help_key);
        if(old_data!=null && old_data.getCur_level()>=req.getHelp_level())
            return res_issue_help.result_has_same;
        guild_help_data new_data=new guild_help_data();
        new_data.setCur_level(req.getHelp_level());
        new_data.setSrc_namespace(req.getPlayer_namespace());
        new_data.setSrc_id(req.getPlayer_id());
        new_data.setMax_help_count(req.getMax_help_count());
        new_data.setMax_coin_count(req.getMax_coin_count());
        new_data.setHelp_type_id(req.getHelp_id());
        datas.put(this_help_key,new_data);
        return  res_issue_help.result_ok;
    }
    public void build_info_list(req_get_help_list req, res_get_guild_help_list res){

        if(res.getMyself()==null)
            res.setMyself(new ArrayList<>());
        if(res.getOthers()==null)
            res.setOthers(new ArrayList<>());

        datas.entrySet().stream().forEach(e->{

            res_get_guild_help_component c=new res_get_guild_help_component();

            c.setHelp_count((int)e.getValue().help_members.size());
            c.setHelp_id(e.getValue().getHelp_type_id());
            c.setMax_help_count(e.getValue().max_help_count);
            c.setPlayer_id(e.getValue().getSrc_id());
            c.setPlayer_namespace(e.getValue().src_namespace);

            if(e.getValue().getSrc_namespace().equals(req.getPlayer_namespace())
                    && e.getValue().getSrc_id().equals(req.getPlayer_id())
            ){
                res.getMyself().add(c);
            }
            else if(!e.getValue()
                    .getHelp_members()
                    .contains(req.getPlayer_namespace().concat(".").concat(req.getPlayer_id()))
            ){
                res.getOthers().add(c);
            }
        });
    }

    public int do_guild_help(req_do_guild_help req){
        guild_help_data data=datas.get(req.getTarget_namespace().concat("_")
                .concat(req.getTarget_id().concat("_").concat(req.getTarget_help_id())));
        if(data==null)
            return res_do_guild_help.unknown_help_id;
        if(data.getHelp_members().size()>=data.getMax_help_count())
            return res_do_guild_help.full_count;
        if(data.getCur_level()!=req.getTarget_help_level())
            return res_do_guild_help.level_unmatched;
        if(data.getHelp_members().stream().anyMatch(o->o.equals(
                req.getPlayer_namespace().concat(".").concat(req.getPlayer_id()))))
            return res_do_guild_help.helped;
        data.setCur_level(data.getCur_level()+1);
        data.getHelp_members().add(req.getPlayer_namespace().concat(".").concat(req.getPlayer_id()));
        return res_do_guild_help.result_ok;
    }
    public void del_guild_help(String player_namespace,String player_id){
        List<String> l=datas.entrySet().stream()
                .filter(e->{
                    return e.getValue().getSrc_namespace().equals(player_namespace)&&e.getValue().getSrc_id().equals(player_id);
                })
                .map(e->e.getKey()).collect(Collectors.toList());
        l.forEach(e->{datas.remove(e);});
    }
    public void del_guild_help(guild_member who){
        del_guild_help(who.player_namespace,who.player_id);
    }
    public void del_guild_help(req_del_guild_help req){
        datas.remove(req.getTarget_namespace().concat("_")
                .concat(req.getTarget_id().concat("_").concat(req.getTarget_help_id())));
    }
}
