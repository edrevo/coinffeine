package com.bitwise.bitmarket.common.currency;

public enum Currency {

    EUR("EUR"), USD("USD");

    private final String currency;

    private Currency(final String currency) {
        this.currency = currency;
    }

    @Override
    public String toString() {
        return this.currency;
    }
}
