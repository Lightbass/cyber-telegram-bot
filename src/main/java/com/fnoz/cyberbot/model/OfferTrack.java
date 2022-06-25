package com.fnoz.cyberbot.model;

public class OfferTrack {
    public final String link;
    public final int minPrice;
    public volatile String lastOffer;

    public OfferTrack(String link, String lastOffer) {
        this(link, lastOffer, 999999999);
    }

    public OfferTrack(String link, String lastOffer, int minPrice) {
        this.link = link;
        this.lastOffer = lastOffer;
        this.minPrice = minPrice;
    }
}
