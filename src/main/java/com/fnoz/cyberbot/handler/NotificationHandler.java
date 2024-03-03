package com.fnoz.cyberbot.handler;

import org.glassfish.grizzly.utils.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.objects.Message;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.function.Consumer;

import static com.fnoz.cyberbot.tools.TelegramUtils.sendMessage;

public class NotificationHandler implements Consumer<Message> {

    private static final Logger logger = LoggerFactory.getLogger(NotificationHandler.class);

    private final TelegramLongPollingBot bot;

    private final Map<Long, Pair<Long, Long>> userList = new HashMap<>();

    public NotificationHandler(TelegramLongPollingBot bot) {
        this.bot = bot;
        new Thread(() -> {
            while (true) {
                try {
                    Thread.sleep(1000);
                    userList.forEach((key, value) -> {
                        if (System.currentTimeMillis() > value.getSecond()) {
                            value.setSecond(System.currentTimeMillis() + value.getFirst());
                            sendMessage(key.toString(), "Встань, присядь!", bot);
                        }
                    });
                } catch (Exception e) {
                    logger.error("Error: {}", e.getMessage());
                }
            }
        }).start();
    }

    @Override
    public void accept(Message message) {
        if (message.hasText() && message.getText().matches("^/notification\\s\\d+$")) {
            long delayMillis = Long.parseLong(message.getText().split(" ")[1]) * 60 * 1000;
            Long nextNotification = System.currentTimeMillis() + delayMillis;
            userList.put(message.getChatId(), new Pair<>(delayMillis, nextNotification));
        }
    }
}
