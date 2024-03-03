package com.fnoz.cyberbot.model;

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
}
