package com.fnoz.cyberbot.handler;

import com.fnoz.cyberbot.service.MinecraftService;
import com.fnoz.cyberbot.service.YandexApiService;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.GetFile;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.DeleteMessage;
import org.telegram.telegrambots.meta.api.objects.File;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.PhotoSize;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

public class MemePhotoSaverHandler extends TelegramLongPollingBot {

    private final String BOT_USERNAME;
    private final String BOT_TOKEN;
    private final String DUPLICATE_ANSWER = ". Дубликат удалён";
    private final String IMAGE_SAVED = "Изображение сохранено.";
    private final String NO_IMAGE = ". Нет изображения.";

    private final int DELETE_MESSAGE_DELAY_SEC;
    private final int FORWARDED_MESSAGES_CACHE;

    private final YandexApiService yandexApi;
    private final MinecraftService minecraftService;
    private final LinkedHashSet<String> chatMessagesIds;

    public MemePhotoSaverHandler(Properties properties) {
        this.BOT_USERNAME = properties.getProperty("fnoz.bot.username");
        this.BOT_TOKEN = properties.getProperty("fnoz.bot.token");
        this.DELETE_MESSAGE_DELAY_SEC = Integer.parseInt(properties.getProperty("fnoz.message-delay-sec"));
        this.FORWARDED_MESSAGES_CACHE = Integer.parseInt(properties.getProperty("fnoz.forwarded-messages-cache"));

        chatMessagesIds = new LinkedHashSet<>(FORWARDED_MESSAGES_CACHE);
        yandexApi = new YandexApiService(properties.getProperty("fnoz.yandex.token"));
        minecraftService = new MinecraftService(properties.getProperty("fnoz.minecraft.server.ip"));
    }

    @Override
    public void onUpdateReceived(Update update) {
        if (update.hasMessage()) {
            checkDuplicateMessage(update.getMessage());
            processMemeMessage(update.getMessage());
            checkMinecraftPlayers(update.getMessage());
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

    private void checkDuplicateMessage(Message message) {
        String parentId = null;
        if (message.getForwardFrom() != null) {
            int textHash = message.hasText() ? message.getText().hashCode() : 0;
            int photoHash = message.hasPhoto() ?
                    message.getPhoto().stream().mapToInt(a -> a.getFileUniqueId().hashCode()).reduce(0, (a, b) -> a ^ b) : 0;
            int sum = textHash ^ photoHash ^ message.getForwardDate();
            parentId = "u" + message.getForwardFrom().getId() + "m" + sum;
        } else if (message.getForwardFromChat() != null) {
            parentId = "c" + message.getForwardFromChat().getId() + "m" + message.getForwardFromMessageId();
        }
        if (parentId != null) {
            if (chatMessagesIds.size() > FORWARDED_MESSAGES_CACHE - 1) {
                Iterator it = chatMessagesIds.iterator();
                it.next();
                it.remove();
            }
            if (chatMessagesIds.contains(parentId)) {
                DeleteMessage.DeleteMessageBuilder deleteMessageBuilder = DeleteMessage.builder()
                        .chatId(message.getChatId().toString())
                        .messageId(message.getMessageId());
                try {
                    execute(deleteMessageBuilder.build());
                } catch (TelegramApiException e) {
                    e.printStackTrace();
                }
                sendMessage(message.getChatId().toString(), "@" + message.getFrom().getUserName() + DUPLICATE_ANSWER);
            } else {
                chatMessagesIds.add(parentId);
            }
        }
    }

    private void processMemeMessage(Message message) {
        String caption = message.getCaption();
        if (message.hasPhoto() && caption != null && caption.contains("/meme")) {

            PhotoSize pz = message.getPhoto().stream()
                    .max(Comparator.comparingInt(PhotoSize::getFileSize)).orElse(null);

            if (pz != null) {
                if (pz.getFilePath() != null) {
                    System.out.println(pz.getFilePath());
                } else {
                    GetFile getFile = new GetFile(pz.getFileId());
                    try {
                        File file = execute(getFile);
                        java.io.File localFile = downloadPhotoByFilePath(file.getFilePath());
                        String cloudFileName = LocalDate.now().toString() + "_" + file.getFileUniqueId() + ".jpg";
                        yandexApi.uploadFile(localFile.getAbsolutePath(), cloudFileName);
                        sendMessage(message.getChatId().toString(), IMAGE_SAVED);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        } else if (message.hasText() && message.getText().contains("/meme")) {
            DeleteMessage.DeleteMessageBuilder deleteMessageBuilder = DeleteMessage.builder()
                    .chatId(message.getChatId().toString())
                    .messageId(message.getMessageId());
            try {
                execute(deleteMessageBuilder.build());
            } catch (TelegramApiException e) {
                e.printStackTrace();
            }
            sendMessage(message.getChatId().toString(), "@" + message.getFrom().getUserName() + NO_IMAGE);
        }
    }

    private void checkMinecraftPlayers(Message message) {
        if (message.hasText() && message.getText().contains("/mine")) {
            String playerList =
                    Arrays.stream(minecraftService.getOnlineUsernames()).reduce("", (a, b) -> a + "\n" + b);
            sendMessage(message.getChatId().toString(), playerList);
        }
    }

    private void sendMessage(String chatId, String text) {
        SendMessage sendMessage = SendMessage.builder()
                .chatId(chatId)
                .text(text)
                .build();

        try {
            Message message = execute(sendMessage);
            DeleteMessage deleteMessage = DeleteMessage.builder()
                    .chatId(chatId)
                    .messageId(message.getMessageId())
                    .build();
            TimeUnit.SECONDS.sleep(DELETE_MESSAGE_DELAY_SEC);
            execute(deleteMessage);
        } catch (TelegramApiException | InterruptedException e) {
            e.printStackTrace();
        }
    }

    private java.io.File downloadPhotoByFilePath(String filePath) {
        try {
            return downloadFile(filePath);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
        return null;
    }
}
