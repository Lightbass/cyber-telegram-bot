package com.fnoz.cyberbot.model;

import org.ton.java.address.Address;
import org.ton.java.cell.Cell;
import org.ton.java.cell.CellSlice;

import java.math.BigInteger;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

public class OfferInfo {
    public int price;
    public String title;
    public String description;
    public String date;

    public OfferInfo(int price, String title, String description, String date) {
        this.price = price;
        this.title = title;
        this.description = description;
        this.date = date;
    }

    public boolean isOutdated(int minutesOutdated) {
        if (date.contains("секунд")) {
            return false;
        } else if (date.contains("минут")) {
            return Integer.parseInt(date.split(" ")[0]) > minutesOutdated;
        }
        return true;
    }

    @Override
    public String toString() {
        return "Название: " + title + "\nЦена: " + (price == -1 ? "Нет цены" : price) + "\n\nОписание: " + description;
    }

    public static void main(String[] args) throws Exception {


        final HttpClient client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(30))
                .executor(Executors.newFixedThreadPool(1))
                .build();

        HttpRequest request = HttpRequest.newBuilder()
                .timeout(Duration.ofSeconds(30))
                .uri(URI.create("https://dton.io/graphql/"))
                .header("Accept", "application/json, multipart/mixed")
                .header("content-type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString( "{\"query\":\"{\\n  transactions(in_msg_op_code: 1215991425) {\\n    in_msg_body\\n  }\\n}\"}"))
                .build();
        String response = client.send(request, HttpResponse.BodyHandlers.ofString()).body();
        //System.out.println(response);

        String[] base64strings = response.split("\"in_msg_body\":\"");
        List<String> base64strList = Arrays.stream(base64strings)
                .skip(1)
                .map(str -> str.split("\"")[0]).collect(Collectors.toList());
        for (String base64str : base64strList) {
            //byte[] data = Base64.getDecoder().decode("te6ccuEBAwEATAAgkJgCGEh6joEAAAAAAAAADQECAGuADkfXdpXiUxDdbx1gg/SoCd2JjSttbpL8tr2sJfwYni0KTcPzjAClJsC1QAAgAAVGAAADhBAAAwBsXCgbLA==");
            byte[] data = Base64.getDecoder().decode(base64str);
            Cell beginCell = Cell.fromBoc(data);
            CellSlice beginCellSlice = CellSlice.beginParse(beginCell);
            BigInteger opCode = beginCellSlice.loadUint(32);
            BigInteger queryId = beginCellSlice.loadUint(64);
            Cell cellRef = beginCellSlice.loadRef();
            CellSlice refCellSlice = CellSlice.beginParse(cellRef);
            Address address = refCellSlice.loadAddress();
            BigInteger minBid = refCellSlice.loadCoins();
            BigInteger maxBid = refCellSlice.loadCoins();
            BigInteger minBidStep = refCellSlice.loadUint(8);
            BigInteger minExtendTime = refCellSlice.loadUint(32);
            BigInteger duration = refCellSlice.loadUint(32);
            if (maxBid.compareTo(new BigInteger("110000000000")) <= 0) {
                System.out.println("--------------------");
                System.out.println("MIN BID: " + minBid);
                System.out.println("MAX BID: " + maxBid);
                System.out.println("address: " + address);
                System.out.println("queryId: " + queryId);
                System.out.println("minBidStep: " + minBidStep);
                System.out.println("minExtendTime: " + minExtendTime);
                System.out.println("duration: " + duration);
            }
        }
    }
}
