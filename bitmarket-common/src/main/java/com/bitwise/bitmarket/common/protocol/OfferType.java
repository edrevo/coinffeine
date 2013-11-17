package com.bitwise.bitmarket.common.protocol;

public enum OfferType {

    BUY_OFFER("buy"),
    SELL_OFFER("sell");

    private final String label;

    OfferType(String label) {
        this.label = label;
    }

    @Override
    public String toString() { return this.label; }
}
