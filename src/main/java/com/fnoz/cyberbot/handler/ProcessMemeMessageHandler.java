package com.fnoz.cyberbot.handler;

import com.fnoz.cyberbot.service.YandexApiService;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.GetFile;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.DeleteMessage;
import org.telegram.telegrambots.meta.api.objects.File;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.PhotoSize;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.function.Consumer;

import static com.fnoz.cyberbot.tools.TelegramUtils.downloadPhotoByFilePath;
import static com.fnoz.cyberbot.tools.TelegramUtils.sendTempMessage;

public class ProcessMemeMessageHandler implements Consumer<Message> {

    private final TelegramLongPollingBot bot;
    private final YandexApiService yandexApiService;
    private final int deleteMessageDelaySec;

    private final String imageSavedText = "Изображение сохранено.";
    private final String noImageText = ". Нет изображения.";

    public ProcessMemeMessageHandler(TelegramLongPollingBot bot, YandexApiService yandexApiService, int deleteMessageDelaySec) {
        this.bot = bot;
        this.yandexApiService = yandexApiService;
        this.deleteMessageDelaySec = deleteMessageDelaySec;
    }

    @Override
    public void accept(Message message) {
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
                        File file = bot.execute(getFile);
                        java.io.File localFile = downloadPhotoByFilePath(file.getFilePath(), bot);
                        String cloudFileName = LocalDate.now().toString() + "_" + file.getFileUniqueId() + ".jpg";
                        yandexApiService.uploadFile(localFile.getAbsolutePath(), cloudFileName);
                        sendTempMessage(message.getChatId().toString(), imageSavedText, deleteMessageDelaySec, bot);
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
                bot.execute(deleteMessageBuilder.build());
            } catch (TelegramApiException e) {
                e.printStackTrace();
            }
            sendTempMessage(message.getChatId().toString(), "@" + message.getFrom().getUserName() + noImageText, deleteMessageDelaySec, bot);
        }
    }
}
