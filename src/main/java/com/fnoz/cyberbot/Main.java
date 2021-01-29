package com.fnoz.cyberbot;

import com.fnoz.cyberbot.handler.MemePhotoSaverHandler;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;

import java.io.IOException;
import java.net.URL;
import java.util.Properties;

public class Main {

    public static void main(String[] args) throws IOException {
        try {
            TelegramBotsApi botsApi = new TelegramBotsApi(DefaultBotSession.class);
            botsApi.registerBot(new MemePhotoSaverHandler(getProperties()));
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    private static Properties getProperties() throws IOException {
        Properties properties = new Properties();
        URL resourceUrl = Thread.currentThread().getContextClassLoader().getResource("application.properties");
        properties.load(resourceUrl.openStream());
        return properties;
    }
}
