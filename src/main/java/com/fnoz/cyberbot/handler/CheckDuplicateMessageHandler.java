package com.fnoz.cyberbot.handler;

import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.DeleteMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Properties;
import java.util.function.Consumer;

import static com.fnoz.cyberbot.tools.TelegramUtils.sendTempMessage;

public class CheckDuplicateMessageHandler implements Consumer<Message> {

    private final TelegramLongPollingBot bot;
    private final int deleteMessageDelaySec;
    private final int forwardedMessagesCache;
    private final LinkedHashSet<String> chatMessagesIds;


    public CheckDuplicateMessageHandler(TelegramLongPollingBot bot, Properties properties) {
        this.bot = bot;
        this.deleteMessageDelaySec = Integer.parseInt(properties.getProperty("fnoz.message-delay-sec"));
        this.forwardedMessagesCache = Integer.parseInt(properties.getProperty("fnoz.forwarded-messages-cache"));
        this.chatMessagesIds = new LinkedHashSet<>(forwardedMessagesCache);
    }

    @Override
    public void accept(Message message) {
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
            if (chatMessagesIds.size() > forwardedMessagesCache - 1) {
                Iterator it = chatMessagesIds.iterator();
                it.next();
                it.remove();
            }
            if (chatMessagesIds.contains(parentId)) {
                DeleteMessage.DeleteMessageBuilder deleteMessageBuilder = DeleteMessage.builder()
                        .chatId(message.getChatId().toString())
                        .messageId(message.getMessageId());
                try {
                    bot.execute(deleteMessageBuilder.build());
                } catch (TelegramApiException e) {
                    e.printStackTrace();
                }
                String DUPLICATE_ANSWER = ". Дубликат удалён";
                sendTempMessage(message.getChatId().toString(), "@" + message.getFrom().getUserName() + DUPLICATE_ANSWER, deleteMessageDelaySec, bot);
            } else {
                chatMessagesIds.add(parentId);
            }
        }
    }
}
