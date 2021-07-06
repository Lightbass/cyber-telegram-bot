package com.fnoz.cyberbot.tools;

import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.DeleteMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.util.concurrent.TimeUnit;

public class TelegramUtils {
    public static void sendTempMessage(String chatId, String text, long lifeTime, TelegramLongPollingBot bot) {
        try {
            Message message = sendMessage(chatId, text, bot);
            TimeUnit.SECONDS.sleep(lifeTime);
            deleteMessage(message, bot);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public static Message sendMessage(String chatId, String text, TelegramLongPollingBot bot) {
        try {
            SendMessage sendMessage = SendMessage.builder()
                    .chatId(chatId)
                    .text(text)
                    .build();
            return bot.execute(sendMessage);
        } catch (TelegramApiException e) {
            e.printStackTrace();
            return null;
        }
    }

    public static void deleteMessage(Message message, TelegramLongPollingBot bot) {
        try {
            if (message != null) {
                DeleteMessage deleteMessage = DeleteMessage.builder()
                        .chatId(message.getChatId().toString())
                        .messageId(message.getMessageId())
                        .build();
                bot.execute(deleteMessage);
            }
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    public static void editMessage(Message message, String newText, TelegramLongPollingBot bot) {
        try {
            if (message != null && !message.getText().equals(newText)) {
                EditMessageText editMessageText = EditMessageText.builder()
                        .chatId(message.getChatId().toString())
                        .messageId(message.getMessageId())
                        .text(newText)
                        .build();
                bot.execute(editMessageText);
            }
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    public static java.io.File downloadPhotoByFilePath(String filePath, TelegramLongPollingBot bot) {
        try {
            return bot.downloadFile(filePath);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
        return null;
    }
}
