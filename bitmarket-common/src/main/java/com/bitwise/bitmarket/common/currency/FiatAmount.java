package com.bitwise.bitmarket.common.currency;

import java.math.BigDecimal;
import java.util.Currency;

public class FiatAmount {

    private final BigDecimal amount;
    private final Currency currency;

    public FiatAmount(BigDecimal amount, Currency currency) {
        this.amount = amount;
        this.currency = currency;
    }

    public BigDecimal getAmount() {
        return this.amount;
    }

    public Currency getCurrency() {
        return this.currency;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result
                + (this.amount == null ? 0 : this.amount.hashCode());
        result = prime * result
                + (this.currency == null ? 0 : this.currency.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (this.getClass() != obj.getClass()) {
            return false;
        }
        FiatAmount other = (FiatAmount) obj;
        if (this.amount == null) {
            if (other.amount != null) {
                return false;
            }
        } else if (!this.amount.equals(other.amount)) {
            return false;
        }
        if (this.currency == null) {
            if (other.currency != null) {
                return false;
            }
        } else if (!this.currency.equals(other.currency)) {
            return false;
        }
        return true;
    }
}
