package com.check24.internetcomparison.model;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

public class InternetOffer {
    private String provider;
    private String product;
    private double price;

    public InternetOffer() {}

    public InternetOffer(String provider, String product, double price) {
        this.provider = provider;
        this.product = product;
        this.price = price;
    }

    public String getProvider() {
        return provider;
    }

    public void setProvider(String provider) {
        this.provider = provider;
    }

    public String getProduct() {
        return product;
    }

    public void setProduct(String product) {
        this.product = product;
    }

    public double getPrice() {
        return price;
    }

    public void setPrice(double price) {
        this.price = price;
    }

    public String getFormattedPrice() {
        DecimalFormatSymbols symbols = new DecimalFormatSymbols(Locale.GERMANY);
        DecimalFormat formatter = new DecimalFormat("#,##0.00", symbols);
        return formatter.format(price);
    }
}
