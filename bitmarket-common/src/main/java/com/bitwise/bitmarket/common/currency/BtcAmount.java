package com.bitwise.bitmarket.common.currency;

import java.math.BigDecimal;

public class BtcAmount {

    private final BigDecimal amount;

    public BtcAmount(BigDecimal amount) {
        super();
        this.amount = amount;
    }

    public BigDecimal getAmount() {
        return this.amount;
    }

    @Override
    public String toString() {
        return String.format("%s BTC", this.amount.toPlainString());
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + (this.amount == null ? 0 : this.amount.hashCode());
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
        final BtcAmount other = (BtcAmount) obj;
        if (this.amount == null) {
            if (other.amount != null) {
                return false;
            }
        } else if (!this.amount.equals(other.amount)) {
            return false;
        }
        return true;
    }
}
