package com.fnoz.cyberbot.model;

public class AuctionInfo {
    public int price;
    public int timeRemaining;
    public String numberLink;

    public AuctionInfo(int price, int timeRemaining, String numberLink) {
        this.price = price;
        this.timeRemaining = timeRemaining;
        this.numberLink = numberLink;
    }

    @Override
    public String toString() {
        return "Телефон: " + numberLink + " Цена: " + price + " Время: " + timeRemaining;
    }
}
