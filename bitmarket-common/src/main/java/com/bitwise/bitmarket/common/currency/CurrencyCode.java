package com.bitwise.bitmarket.common.currency;

public enum CurrencyCode {

    EUR("EUR"), USD("USD"), BTC("BTC");

    private final String currency;

    private CurrencyCode(final String currency) {
        this.currency = currency;
    }

    @Override
    public String toString() {
        return this.currency;
    }
}
