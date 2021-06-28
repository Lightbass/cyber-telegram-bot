package com.fnoz.cyberbot.service;

import com.fnoz.cyberbot.tools.Query;

/**
 * Send a query to a given minecraft server and store any metadata and the
 * player list.
 *
 * @author Ryan Shaw, Jonas Konrad
 */
public class MinecraftService {

    private final Query query;

    public MinecraftService(String ip) {
        query = new Query(ip, 25565, 25565);
    }

    public String[] getOnlineUsernames() {
        query.sendQueryRequest();
        return query.getOnlineUsernames();
    }
}
