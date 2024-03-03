package com.fnoz.cyberbot;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URL;
import java.util.Properties;

public class Main {

    private static final Logger logger = LoggerFactory.getLogger(Main.class);

    public static void main(String[] args) throws IOException {
        try {
            logger.info("Start bot");
            TelegramBotsApi botsApi = new TelegramBotsApi(DefaultBotSession.class);
            botsApi.registerBot(new TelegramBot(getProperties()));
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    private static Properties getProperties() throws IOException {
        Properties properties = new Properties();
        File externalProps = new File("application.properties");
        if (externalProps.isFile()) {
            properties.load(new FileInputStream(externalProps));
        } else {
            URL resourceUrl = Thread.currentThread().getContextClassLoader().getResource("application.properties");
            properties.load(resourceUrl.openStream());
        }
        return properties;
    }
}
