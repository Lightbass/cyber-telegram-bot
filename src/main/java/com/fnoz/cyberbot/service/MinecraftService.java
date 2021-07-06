package com.fnoz.cyberbot.service;

import com.fnoz.cyberbot.tools.MinecraftQuery;

import java.util.Arrays;

/**
 * Send a query to a given minecraft server and store any metadata and the
 * player list.
 *
 * @author Ryan Shaw, Jonas Konrad
 */
public class MinecraftService {

    private final MinecraftQuery minecraftQuery;

    public MinecraftService(String ip) {
        minecraftQuery = new MinecraftQuery(ip, 25565, 25565);
    }

    public String getOnlineUsernames() {
        minecraftQuery.sendQueryRequest();
        String[] playerList = minecraftQuery.getOnlineUsernames();
        String playerListString = Arrays.stream(playerList)
                .reduce("", (a, b) -> a + "\n" + b);
        playerListString = "#mine\nСписок игроков:\n" +
                (playerListString.isEmpty() ? "\nНикого" : playerListString);
        return playerListString;
    }
}
