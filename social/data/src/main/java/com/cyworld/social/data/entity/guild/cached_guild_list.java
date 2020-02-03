package com.cyworld.social.data.entity.guild;

import lombok.Data;

import java.util.TreeSet;

public class cached_guild_list {

    @Data
    public static class cached_guild_data implements Comparable<cached_guild_data>{
        String id;
        String name;
        String language;
        String domain_id;
        long power;
        int level_for_join;
        int max_member_count;
        int cur_member_count;
        boolean opening=true;
        @Override
        public int compareTo(cached_guild_data other) {
            if (other.power==this.getPower())
                return this.name.compareTo(other.name);
            return Long.compare(other.power,this.power);
        }
    }
    public TreeSet<cached_guild_data> cahce=new TreeSet<>();
}
