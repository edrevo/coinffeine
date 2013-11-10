package com.bitwise.bitmarket.common.currency;

import java.util.Currency;

public class Amount {

    private final float amount;
    private final Currency currency;

    public Amount(float amount, Currency currency) {
        this.amount = amount;
        this.currency = currency;
    }

    public float getAmount() {
        return this.amount;
    }

    public Currency getCurrency() {
        return this.currency;
    }
}
