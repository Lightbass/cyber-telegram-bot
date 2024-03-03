package com.fnoz.cyberbot;

import com.fnoz.cyberbot.handler.AvitoTrackerHandler;
import com.fnoz.cyberbot.handler.CheckDuplicateMessageHandler;
import com.fnoz.cyberbot.handler.MinecraftServerStatusHandler;
import com.fnoz.cyberbot.handler.NotificationHandler;
import com.fnoz.cyberbot.handler.ProcessMemeMessageHandler;
import com.fnoz.cyberbot.handler.SendAllHandler;
import com.fnoz.cyberbot.service.MinecraftService;
import com.fnoz.cyberbot.service.YandexApiService;
import org.glassfish.jersey.internal.util.Producer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import java.util.function.Consumer;

public class TelegramBot extends TelegramLongPollingBot {

    private static final Logger logger = LoggerFactory.getLogger(TelegramBot.class);

    private final String BOT_USERNAME;
    private final String BOT_TOKEN;

    private final List<Consumer<Message>> handlers = new ArrayList<>();


    public TelegramBot(Properties properties) {
        this.BOT_USERNAME = properties.getProperty("fnoz.bot.username");
        this.BOT_TOKEN = properties.getProperty("fnoz.bot.token");

        String enabledHandlers = properties.getProperty("fnoz.enabled-handlers");
        List<String> handlersList = Arrays.asList(enabledHandlers.split(","));
        addToHandlerIfEnabled("duplicate", () -> new CheckDuplicateMessageHandler(TelegramBot.this, properties), handlersList);
        addToHandlerIfEnabled("meme", () -> new ProcessMemeMessageHandler(TelegramBot.this, properties), handlersList);
        addToHandlerIfEnabled("minecraft", () -> new MinecraftServerStatusHandler(TelegramBot.this, properties), handlersList);
        addToHandlerIfEnabled("send-all", () -> new SendAllHandler(TelegramBot.this, properties), handlersList);
        addToHandlerIfEnabled("avito", () -> new AvitoTrackerHandler(TelegramBot.this), handlersList);
        addToHandlerIfEnabled("notification", () -> new NotificationHandler(TelegramBot.this), handlersList);
    }

    @Override
    public void onUpdateReceived(Update update) {
        if (update.hasMessage()) {
            this.handlers.forEach(handler -> handler.accept(update.getMessage()));
        }
    }

    @Override
    public String getBotUsername() {
        return BOT_USERNAME;
    }

    @Override
    public String getBotToken() {
        return BOT_TOKEN;
    }

    private void addToHandlerIfEnabled(String tag, Producer<Consumer<Message>> handler, List<String> enabledHandlers) {
        if (enabledHandlers.contains(tag)) {
            handlers.add(handler.call());
            logger.info("Handler with tag:{} enabled", tag);
        }
    }
}
