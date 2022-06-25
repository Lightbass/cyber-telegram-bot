package com.fnoz.cyberbot.handler;

import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.objects.Message;

import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;

import static com.fnoz.cyberbot.tools.TelegramUtils.sendTempMessage;

public class SendAllHandler implements Consumer<Message> {

    private final TelegramLongPollingBot bot;
    private final int deleteMessageDelaySec;

    private final List<String> userList = Arrays.asList("bassok", "deddok", "Shivinskiy", "s0n1c13", "snakoff", "sanchez752");

    public SendAllHandler(TelegramLongPollingBot bot, int deleteMessageDelaySec) {
        this.bot = bot;
        this.deleteMessageDelaySec = deleteMessageDelaySec;
    }

    @Override
    public void accept(Message message) {
        if (message.hasText() && message.getText().matches(".*(^|\\s)@all($|\\s).*") && message.getChat().isSuperGroupChat()) {
            sendTempMessage(
                    message.getChatId().toString(),
                    userList.stream().map(s -> "@" + s).reduce("", (a, b) -> a + " " + b),
                    deleteMessageDelaySec, bot);
        }
    }
}
