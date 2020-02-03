package com.cyworld.social.test;

import lombok.Builder;

import java.util.ArrayList;
import java.util.List;

public class static_data {
    @Builder
    public static class server{
        public String name;
    }
    @Builder
    public static class player{
        public String server_name;
        public String player_id;
        public String guild_id;
    }

    public static List<server> servers=new ArrayList<>();
    public static List<player> players=new ArrayList<>();

    public static void init(){
        servers.add(server.builder().name("s001").build());
        servers.add(server.builder().name("s002").build());
        servers.add(server.builder().name("s003").build());
        servers.add(server.builder().name("s004").build());
        servers.add(server.builder().name("s005").build());

        players.add(player.builder().server_name("s001").player_id("u001").build());
        players.add(player.builder().server_name("s001").player_id("u002").build());
        players.add(player.builder().server_name("s001").player_id("u003").build());
        players.add(player.builder().server_name("s001").player_id("u004").build());
        players.add(player.builder().server_name("s001").player_id("u005").build());

        players.add(player.builder().server_name("s002").player_id("u001").build());
        players.add(player.builder().server_name("s002").player_id("u002").build());
        players.add(player.builder().server_name("s002").player_id("u003").build());
        players.add(player.builder().server_name("s002").player_id("u004").build());
        players.add(player.builder().server_name("s002").player_id("u005").build());

        players.add(player.builder().server_name("s003").player_id("u001").build());
        players.add(player.builder().server_name("s003").player_id("u002").build());
        players.add(player.builder().server_name("s003").player_id("u003").build());
        players.add(player.builder().server_name("s003").player_id("u004").build());
        players.add(player.builder().server_name("s003").player_id("u005").build());

        players.add(player.builder().server_name("s004").player_id("u001").build());
        players.add(player.builder().server_name("s004").player_id("u002").build());
        players.add(player.builder().server_name("s004").player_id("u003").build());
        players.add(player.builder().server_name("s004").player_id("u004").build());
        players.add(player.builder().server_name("s004").player_id("u005").build());

        players.add(player.builder().server_name("s005").player_id("u001").build());
        players.add(player.builder().server_name("s005").player_id("u002").build());
        players.add(player.builder().server_name("s005").player_id("u003").build());
        players.add(player.builder().server_name("s005").player_id("u004").build());
        players.add(player.builder().server_name("s005").player_id("u005").build());
    }
}
