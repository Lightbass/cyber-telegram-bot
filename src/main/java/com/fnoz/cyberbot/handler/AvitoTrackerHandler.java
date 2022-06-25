package com.fnoz.cyberbot.handler;

import com.fnoz.cyberbot.model.OfferInfo;
import com.fnoz.cyberbot.model.OfferTrack;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.fnoz.cyberbot.tools.TelegramUtils.sendMessage;

public class AvitoTrackerHandler implements Consumer<Message> {

    private final TelegramLongPollingBot bot;
    private final Map<Long, List<OfferTrack>> linksToTrack = new HashMap<>();

    private final int refreshOffersTime = 60000;
    private volatile Thread trackerThread;


    public AvitoTrackerHandler(TelegramLongPollingBot bot) {
        this.bot = bot;
    }

    @Override
    public void accept(Message message) {
        if (message.hasText() && message.getText().matches("^https://www\\.avito\\.ru/.*")) {
            linksToTrack.computeIfAbsent(message.getChatId(), id -> new ArrayList<>());
            linksToTrack.compute(message.getChatId(), (key, value) -> {
                String link = message.getText();
                int minPrice = 999999999;
                if (link.matches("^https://.* \\d{1,}")) {
                    link = link.split(" ")[0];
                    minPrice = Integer.parseInt(link.split(" ")[1]);
                }
                String lastOffer = checkOffers(link).get(0).title;
                value.add(new OfferTrack(message.getText(), lastOffer, minPrice));
                return value;
            });
            trackerThread();
        } else if (message.hasText() && message.getText().matches("^/avitoDelete(@.+)?\\d+$")) {
            int index = Integer.parseInt(message.getText().split("Delete")[1]);
            OfferTrack offerTrack = linksToTrack.get(message.getChatId()).get(index);
            sendMessage(message.getChatId().toString(), "Отключено отслеживание по ссылке: " + offerTrack.link, bot);
            linksToTrack.get(message.getChatId()).remove(index);


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
        }
    }

    private void trackerThread() {
        if (this.trackerThread != null && this.trackerThread.getState() != Thread.State.TERMINATED) {
            this.trackerThread.interrupt();
            try {
                this.trackerThread.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        this.trackerThread = new Thread(() -> {
            try {
                while (true) {
                    Thread.sleep(refreshOffersTime);

                    linksToTrack.forEach((chatId, offerTracks) -> {
                                for (int trackId = 0; trackId < offerTracks.size(); trackId++) {
                                    OfferTrack offerTrack = offerTracks.get(trackId);
                                    List<OfferInfo> offers = checkOffers(offerTrack.link);
                                    for (int i = 0; i < offers.size() && !offers.get(i).title.equals(offerTrack.lastOffer); i++) {
                                        if (offers.get(i).price < offerTrack.minPrice) {
                                            sendMessage(String.valueOf(chatId), "Ссылка: " + offerTrack.link + "\n\n" + offers.get(i) + "\nКоманда удаления: /avitoDelete" + trackId, bot);
                                        }
                                    }
                                    offerTrack.lastOffer = offers.get(0).title;
                                }
                            }
                    );
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        });
        this.trackerThread.start();
    }

    private String makeRequest(String url) {
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:101.0) Gecko/20100101 Firefox/101.0)")
                .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,*/*;q=0.8")
                .header("Accept-Language", "ru-RU,ru;q=0.8,en-US;q=0.5,en;q=0.3")
                .header("Cache-Control", "no-cache")
                .header("Pragma", "no-cache")
                .GET().build();
        try {
            return client.send(request, HttpResponse.BodyHandlers.ofString()).body();
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
        return "";
    }

    private List<OfferInfo> checkOffers(String url) {
        String[] splits = makeRequest(url).split("title-root_maxHeight");
        List<OfferInfo> offers = new ArrayList<>();
        for (int i = 2; i < splits.length; i += 2) {
            String[] descriptions = splits[i].split("item-description-.*\">");
            String price = splits[i].split("class=\"price-text(.*?)\">")[1].split("<")[0];
            price = price.equals("Не указана") || price.equals("Цена не указана") || price.equals("Цена договорная") || price.equals("Зарплата не указана") || price.equals("Бесплатно") ? null : price;
            if (descriptions.length < 2) {
                continue;
            }
            OfferInfo offerInfo = new OfferInfo(
                    price == null ? -1 : Integer.parseInt(price.replaceAll(" ", "")),
                    splits[i].split(">")[1].split("<")[0],
                    descriptions[1].split("</div>")[0]
            );
            offers.add(offerInfo);
        }
        return offers;
    }

    // Later
    private OfferInfo parseHtml(String body) {
        Pattern p = Pattern.compile(" (\\S+or\\S+) ");
        Matcher m = p.matcher(body);
        m.find();
        String theGroup = m.group(1);
        return null;
    }

    public static void main(String[] args) {
        AvitoTrackerHandler avitoTrackerHandler = new AvitoTrackerHandler(null);
        //avitoTrackerHandler.checkOffers("https://www.avito.ru/sankt-peterburg?q=new+3ds+xl");
        String search = avitoTrackerHandler.makeRequest("https://www.avito.ru/sankt-peterburg?cd=1&q=%D1%81%D1%82%D0%BE%D0%BB+%D0%B1%D0%B5%D1%81%D0%BF%D0%BB%D0%B0%D1%82%D0%BD%D0%BE").split("items-extra")[0];
        //String search = avitoTrackerHandler.makeRequest("https://www.avito.ru/sankt-peterburg/bytovaya_tehnika/dlya_kuhni/mikrovolnovye_pechi-ASgBAgICAkRglk_MB5pP?q=%D0%BC%D0%B8%D0%BA%D1%80%D0%BE%D0%B2%D0%BE%D0%BB%D0%BD%D0%BE%D0%B2%D0%BA%D0%B0").split("items-extra")[0];
        //String search = avitoTrackerHandler.makeRequest("https://www.avito.ru/sankt-peterburg/igry_pristavki_i_programmy/igrovye_pristavki-ASgBAgICAUSSAsoJ?f=ASgBAgECAUSSAsoJAUXGmgwUeyJmcm9tIjozMDAwLCJ0byI6MH0&q=3DS+XL&s=104").split("items-extra")[0];
        List<OfferInfo> offerInfos = avitoTrackerHandler.getOfferInfo(search);
        //List<OfferInfo> offerInfos = avitoTrackerHandler.checkOffers("https://www.avito.ru/sankt-peterburg?cd=1&q=%D1%81%D1%82%D0%BE%D0%BB+%D0%B1%D0%B5%D1%81%D0%BF%D0%BB%D0%B0%D1%82%D0%BD%D0%BE");
        for (int i = 0; i < offerInfos.size(); i++) {
            System.out.println(offerInfos.get(i));
        }

    }

    private List<OfferInfo> getOfferInfo(String body) {
        List<OfferInfo> offers = new ArrayList<>();
        List<String> names = stringsFromParseHtml(body, "//a[contains(@class, 'title-listRedesign')]/h3[@itemprop]", null);
        List<String> descriptions = stringsFromParseHtml(body, "//div[contains(@class, 'item-description') and contains(@class, 'item-text')]", null);
        List<String> prices = stringsFromParseHtml(body, "//span[contains(@class, 'listRedesign')]/span/meta[@itemprop='price']", "content");
        for (int i = 0; i < names.size(); i++) {
            int price = prices.get(i).equals("...") ? -1 : Integer.parseInt(prices.get(i));
            offers.add(new OfferInfo(price, names.get(i), descriptions.get(i)));
        }
        return offers;
    }

    private List<OfferInfo> getOfferInfoOld(String body) {
        List<OfferInfo> offers = new ArrayList<>();
        List<String> names = stringsFromRegex(body, "itemProp=\"name\".*>(.*?)<\\/h3>", 1);
        List<String> descriptions = stringsFromRegex(body, "item-description-.*>([\\s\\S]*?)<\\/div>", 1);
        List<String> prices = stringsFromRegex(body, "itemProp=\"price\" content=\"(\\d*?)\"\\/", 1);
        for (int i = 0; i < names.size(); i++) {
            offers.add(new OfferInfo(Integer.parseInt(prices.get(i)), names.get(i), descriptions.get(i)));
        }
        return offers;
    }

    private List<String> stringsFromRegex(String body, String regex, int groupId) {
        List<String> result = new ArrayList<>();
        Pattern p = Pattern.compile(regex);
        Matcher m = p.matcher(body);
        while (m.find()) {
            result.add(m.group(groupId));
        }
        return result;
    }

    private List<String> stringsFromParseHtml(String body, String expression, String attributeName) {
        List<String> list = new ArrayList<>();
        Document doc = Jsoup.parse(body);
        Elements newsHeadlines = doc.selectXpath(expression);
        for (Element headline : newsHeadlines) {
            list.add(attributeName == null ? headline.text() : headline.attr(attributeName));
        }
        return list;
    }
}
