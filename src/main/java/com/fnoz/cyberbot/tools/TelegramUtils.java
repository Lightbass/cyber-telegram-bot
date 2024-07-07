package com.fnoz.cyberbot.tools;

import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.DeleteMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

public class TelegramUtils {
    public static void sendTempMessage(String chatId, String text, long lifeTime, TelegramLongPollingBot bot) {
        Message message = sendMessage(chatId, text, bot, null);
        CompletableFuture
                .delayedExecutor(lifeTime, TimeUnit.SECONDS)
                .execute(() -> deleteMessage(message, bot));
    }

    public static void sendTempMessageMuted(String chatId, String text, long lifeTime, TelegramLongPollingBot bot) {
        Message message = sendMessageMuted(chatId, text, bot, null);
        CompletableFuture
                .delayedExecutor(lifeTime, TimeUnit.SECONDS)
                .execute(() -> deleteMessage(message, bot));
    }

    public static Message sendMessage(String chatId, String text, TelegramLongPollingBot bot) {
        return sendMessage(chatId, text, bot, null);
    }

    public static Message sendMessageMuted(String chatId, String text, TelegramLongPollingBot bot,
                                      ReplyKeyboardMarkup keyboardMarkup) {
        return sendMessage(chatId, text, bot, keyboardMarkup, false);
    }
    public static Message sendMessage(String chatId, String text, TelegramLongPollingBot bot,
                                      ReplyKeyboardMarkup keyboardMarkup) {
        return sendMessage(chatId, text, bot, keyboardMarkup, true);
    }

    public static Message sendMessage(String chatId, String text, TelegramLongPollingBot bot,
                                      ReplyKeyboardMarkup keyboardMarkup, boolean notification) {
        try {
            SendMessage sendMessage = SendMessage.builder()
                    .chatId(chatId)
                    .disableNotification(!notification)
                    .text(text.length() > 4096 ? text.substring(0, 4096) : text)
                    .build();
            if (keyboardMarkup != null) {
                sendMessage.setReplyMarkup(keyboardMarkup);
            }
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
