package com.fnoz.cyberbot.handler;

import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.util.ArrayList;

public class OldBotHandler extends TelegramLongPollingBot {

    public void onUpdateReceived(Update update) {
        //            ArrayList<KeyboardRow> buttons = new ArrayList<>();
//            linksToTrack.get(message.getChatId()).forEach(offerTrack -> {
//                KeyboardRow keyboardRow = new KeyboardRow();
//                keyboardRow.add(offerTrack.link);
//                buttons.add(keyboardRow);
//            });
//
//            ReplyKeyboardMarkup inBut = new ReplyKeyboardMarkup();
//            inBut.setKeyboard(buttons);
//            sendMessage(message.getChatId().toString(), "Выберите объявление для удаления:", bot, inBut);

        KeyboardRow button = new KeyboardRow();
        button.add("hi");
        button.add("расписание");
        ArrayList<KeyboardRow> arKey = new ArrayList<>();
        arKey.add(button);

        ReplyKeyboardMarkup inBut = new ReplyKeyboardMarkup();
        inBut.setKeyboard(arKey);

        if (update.hasMessage() && update.getMessage().hasText() && update.getMessage().getText().equals("/start")) {

            SendMessage message = SendMessage.builder()
                    .chatId(update.getMessage().getChatId().toString())
                    .text("Hello")
                    .replyMarkup(inBut)
                    .build();


            try {
                execute(message);
            } catch (TelegramApiException e) {
                e.printStackTrace();
            }
        }

        if (update.hasMessage() && update.getMessage().hasText() && update.getMessage().getText().equals("расписание")) {


            SendMessage message = SendMessage.builder()
                    .chatId(update.getMessage().getChatId().toString())
                    .text("РАСПИСАНИЕ ЗДЕСЬ")
                    .build();

            try {
                execute(message);
            } catch (TelegramApiException e) {
                e.printStackTrace();
            }
        }

        System.out.println(update.hasChannelPost());
        System.out.println(update.hasMessage());
        System.out.println(update.hasCallbackQuery());
        System.out.println(update.hasChosenInlineQuery());
        System.out.println(update.hasEditedChannelPost());
        System.out.println(update.hasEditedMessage());
        System.out.println(update.hasInlineQuery());
        System.out.println(update.hasPreCheckoutQuery());
        System.out.println(update.hasShippingQuery());
    }

    @Override
    public String getBotUsername() {
        return "sample_bot";
    }

    @Override
    public String getBotToken() {
        return "0000000000:AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA";
    }
}
