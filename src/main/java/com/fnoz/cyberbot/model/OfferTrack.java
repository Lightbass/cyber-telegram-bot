package com.fnoz.cyberbot.model;

public class OfferTrack {
    public final String link;
    public volatile String lastOffer;

    public OfferTrack(String link, String lastOffer) {
        this.link = link;
        this.lastOffer = lastOffer;
    }
}
