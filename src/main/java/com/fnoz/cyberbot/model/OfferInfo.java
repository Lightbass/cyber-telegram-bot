package com.fnoz.cyberbot.model;

public class OfferInfo {
    public int price;
    public String title;
    public String description;

    public OfferInfo(int price, String title, String description) {
        this.price = price;
        this.title = title;
        this.description = description;
    }

    @Override
    public String toString() {
        return "Название: " + title + "\nЦена: " + (price == -1 ? "Нет цены" : price) + "\n\nОписание: " + description;
    }
}
