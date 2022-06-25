package com.fnoz.cyberbot;

import com.fnoz.cyberbot.handler.AvitoTrackerHandler;
import com.fnoz.cyberbot.handler.CheckDuplicateMessageHandler;
import com.fnoz.cyberbot.handler.MinecraftServerStatusHandler;
import com.fnoz.cyberbot.handler.ProcessMemeMessageHandler;
import com.fnoz.cyberbot.handler.SendAllHandler;
import com.fnoz.cyberbot.service.MinecraftService;
import com.fnoz.cyberbot.service.YandexApiService;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.objects.Update;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.function.Consumer;

public class TelegramBot extends TelegramLongPollingBot {

    private final String BOT_USERNAME;
    private final String BOT_TOKEN;

    private final List<Consumer> handlers;


    public TelegramBot(Properties properties) {
        this.BOT_USERNAME = properties.getProperty("fnoz.bot.username");
        this.BOT_TOKEN = properties.getProperty("fnoz.bot.token");

        this.handlers = new ArrayList<>() {{
            add(new CheckDuplicateMessageHandler(TelegramBot.this, Integer.parseInt(properties.getProperty("fnoz.message-delay-sec")), Integer.parseInt(properties.getProperty("fnoz.forwarded-messages-cache"))));
            add(new ProcessMemeMessageHandler(TelegramBot.this, new YandexApiService(properties.getProperty("fnoz.yandex.token")), Integer.parseInt(properties.getProperty("fnoz.message-delay-sec"))));
            add(new MinecraftServerStatusHandler(TelegramBot.this, new MinecraftService(properties.getProperty("fnoz.minecraft.server.ip"))));
            add(new SendAllHandler(TelegramBot.this, Integer.parseInt(properties.getProperty("fnoz.message-delay-sec"))));
            add(new AvitoTrackerHandler(TelegramBot.this));
        }};
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
}
