package com.fnoz.cyberbot.handler;

import com.fnoz.cyberbot.model.AuctionInfo;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.objects.Message;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

import static com.fnoz.cyberbot.tools.TelegramUtils.sendMessage;

public class FragmentTrackerHandler implements Consumer<Message> {

    private static final Logger logger = LoggerFactory.getLogger(FragmentTrackerHandler.class);

    private final TelegramLongPollingBot bot;
    private final Map<Long, Integer> chatIdForTrack = new ConcurrentHashMap<>();

    private volatile int refreshOffersTimeInSeconds = 15;
    private volatile Thread trackerThread;
    private volatile String lastAuctionNumber = "";
    private volatile String lastSaleNumber = "";

    private final HttpClient client = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(30))
            .executor(Executors.newFixedThreadPool(1))
            .build();

    public FragmentTrackerHandler(TelegramLongPollingBot bot) {
        this.bot = bot;
        trackerThread();
    }

    @Override
    public void accept(Message message) {
        if (message.hasText() && message.getText().matches("^/fragment\\s\\d+$")) {
            Integer minPrice = Integer.parseInt(message.getText().split(" ")[1]);
            chatIdForTrack.put(message.getChatId(), minPrice);
        } else if (message.hasText() && message.getText().matches("^/fragmentTime\\s\\d+$")) {
            refreshOffersTimeInSeconds = Integer.parseInt(message.getText().split(" ")[1]);
        } else if (message.hasText() && message.getText().matches("^/fragmentDelete$")) {
            chatIdForTrack.remove(message.getChatId());
        }
    }

    private void trackerThread() {
        if (this.trackerThread != null && this.trackerThread.getState() != Thread.State.TERMINATED) {
            this.trackerThread.interrupt();
            try {
                this.trackerThread.join();
            } catch (InterruptedException e) {
                logger.error("Error: ", e);
            }
        }
        this.trackerThread = new Thread(() -> {
            try {
                while (true) {
                    Thread.sleep(refreshOffersTimeInSeconds * 1000);
                    chatIdForTrack.forEach((chatId, minPrice) -> {
                        List<AuctionInfo> auctionInfos = getAuctionInfo("https://fragment.com/numbers?sort=ending&filter=auction");
                        List<AuctionInfo> saleInfos = getSaleInfo("https://fragment.com/numbers?sort=price_asc&filter=sale");
                        StringBuilder auctionString = new StringBuilder();
                        StringBuilder saleString = new StringBuilder();
                        List<String> lastAuctionNumbersList = Arrays.asList(lastAuctionNumber.split(";"));
                        List<String> lastSaleNumbersList = Arrays.asList(lastSaleNumber.split(";"));
                        for (AuctionInfo info : auctionInfos) {
                            if (lastAuctionNumbersList.contains(info.numberLink)) {
                                break;
                            }
                            if (info.timeRemaining < 20 && info.price <= minPrice) {
                                auctionString.append(info).append("\n");
                            }
                        }
                        for (AuctionInfo info : saleInfos) {
                            if (lastSaleNumbersList.contains(info.numberLink)) {
                                break;
                            }
                            if (info.price <= minPrice) {
                                saleString.append(info).append("\n");
                            }
                        }
                        lastAuctionNumber = auctionInfos.stream()
                                .limit(10).map(info -> info.numberLink)
                                .reduce((a, b) -> a + ";" + b).orElse(lastAuctionNumber);
                        lastSaleNumber = saleInfos.stream()
                                .limit(10).map(info -> info.numberLink)
                                .reduce((a, b) -> a + ";" + b).orElse(lastSaleNumber);
                        if (auctionString.length() > 0 || saleString.length() > 0) {
                            auctionString.insert(0, "Auctions:\n")
                                    .append(saleString.insert(0, "\n\nSales:\n"));
                            sendMessage(chatId.toString(), auctionString.toString(), bot);
                        }
                    });
                }
            } catch (InterruptedException e) {
                logger.error("Error: ", e);
            } catch (Exception e) {
                logger.error("Error: ", e);
                trackerThread();
            }
        });
        this.trackerThread.start();
    }

    private List<AuctionInfo> getAuctionInfo(String url) {
        List<AuctionInfo> offers = new ArrayList<>();//div[text() < 130]
        Document doc = Jsoup.parse(makeRequest(url));
        List<String> divWithPriceAndTime = stringsFromParseHtml(doc, "//td[@class='thin-last-col']", null);
        // //div[contains(@class,'table-cell-value tm-value icon-before')]
        //List<String> divWithPrice = stringsFromParseHtml(doc, "//div[contains(@class,'table-cell-value tm-value icon-before')]", null);
        List<String> divWithPrice = stringsFromParseHtml(doc, "//a/div[contains(@class, 'tm-value')]", null);
        List<String> tdWithTime = stringsFromParseHtml(doc, "//td[contains(@class,'wide-only')]//div[@class='tm-timer']/time", null);
        List<String> numberLink = stringsFromParseHtml(doc, "//tr[@class='tm-row-selectable']/td[@class='thin-last-col']/a", "href");
        for (int i = 0; i < divWithPrice.size(); i++) {
            if (tdWithTime.get(i).matches("^\\d+ minute.*$")) {
                offers.add(new AuctionInfo(Integer.parseInt(divWithPrice.get(i).replaceAll(",","")),
                        Integer.parseInt(tdWithTime.get(i).split(" minute")[0]), numberLink.get(i)));
            }
        }
        return offers;
    }

    private List<AuctionInfo> getSaleInfo(String url) {
        List<AuctionInfo> offers = new ArrayList<>();//div[text() < 130]
        Document doc = Jsoup.parse(makeRequest(url));
        List<String> divWithPrice = stringsFromParseHtml(doc, "//td[@class='thin-last-col']/a/div[contains(@class, 'tm-value')]", null);
        List<String> numberLink = stringsFromParseHtml(doc, "//tr[@class='tm-row-selectable']/td[@class='thin-last-col']/a", "href");
        for (int i = 0; i < divWithPrice.size(); i++) {
                offers.add(new AuctionInfo(Integer.parseInt(divWithPrice.get(i).replaceAll(",","")), 0, numberLink.get(i)));
        }
        return offers;
    }

    private String makeRequest(String url) {
        HttpRequest request = HttpRequest.newBuilder()
                .timeout(Duration.ofSeconds(30))
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
            logger.error("Error: ", e);
        }
        return "";
    }

    private List<String> stringsFromParseHtml(Document doc, String expression, String attributeName) {
        List<String> list = new ArrayList<>();
        Elements newsHeadlines = doc.selectXpath(expression);
        for (Element headline : newsHeadlines) {
            list.add(attributeName == null ? headline.text() : headline.attr(attributeName));
        }
        return list;
    }

    public static void main(String[] args) {
        FragmentTrackerHandler avitoTrackerHandler = new FragmentTrackerHandler(null);
        String url = "https://fragment.com/numbers?sort=price_asc&filter=sale";
        List<AuctionInfo> AuctionInfos = avitoTrackerHandler.getAuctionInfo(url);
        for (int i = 0; i < AuctionInfos.size(); i++) {
            if (AuctionInfos.get(i).price < 250 && AuctionInfos.get(i).timeRemaining < 10)
            System.out.println(AuctionInfos.get(i));
        }
        System.out.println("AAA");
    }
}
