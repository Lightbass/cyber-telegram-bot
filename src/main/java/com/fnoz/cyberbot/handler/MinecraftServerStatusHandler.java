package com.fnoz.cyberbot.handler;

import com.fnoz.cyberbot.service.MinecraftService;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.objects.Message;

import java.util.Properties;
import java.util.function.Consumer;

import static com.fnoz.cyberbot.tools.TelegramUtils.deleteMessage;
import static com.fnoz.cyberbot.tools.TelegramUtils.editMessage;
import static com.fnoz.cyberbot.tools.TelegramUtils.sendMessage;

public class MinecraftServerStatusHandler implements Consumer<Message> {

    private final TelegramLongPollingBot bot;
    private final MinecraftService minecraftService;

    private volatile Message playerListMessage;
    private volatile Thread playerListThread;

    public MinecraftServerStatusHandler(TelegramLongPollingBot bot, Properties properties) {
        this.bot = bot;
        this.minecraftService = new MinecraftService(properties.getProperty("fnoz.minecraft.server.ip"));
    }

    @Override
    public void accept(Message message) {
        if (message.hasText() && message.getText().matches("^/mine(@.+)?$") && message.getChat().isSuperGroupChat()) {
            deleteMessage(message, bot);
            restartMinecraftPlayersThread(message.getChatId());
        }
    }

    private void restartMinecraftPlayersThread(Long chatId) {
        if (this.playerListThread != null && this.playerListThread.getState() != Thread.State.TERMINATED) {
            this.playerListThread.interrupt();
            try {
                this.playerListThread.join();
            } catch (InterruptedException e) {
                deleteMessage(this.playerListMessage, bot);
            }
        }
        this.playerListMessage = sendMessage(chatId.toString(), minecraftService.getOnlineUsernames(), bot);
        this.playerListThread = new Thread(() -> {
            try {
                while (true) {
                    Thread.sleep(10000);
                    if (playerListMessage != null) {
                        String listPlayers = minecraftService.getOnlineUsernames();
                        editMessage(playerListMessage, listPlayers, bot);
                        playerListMessage.setText(listPlayers);
                    }
                }
            } catch (InterruptedException e) {
                deleteMessage(this.playerListMessage, bot);
                e.printStackTrace();
            }
        });
        this.playerListThread.start();
    }
}
