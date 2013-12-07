package com.bitwise.bitmarket.common.bitcoin;

import java.math.BigDecimal;
import java.security.interfaces.ECKey;

public class TransactionSlot {

    private final BigDecimal BTCAmount;
    private final ECKey ownerPublicKey;

    TransactionSlot(BigDecimal BTCAmount, ECKey ownerPublicKey) {
        this.BTCAmount = BTCAmount;
        this.ownerPublicKey = ownerPublicKey;
    }

    public BigDecimal getAmount() {
        return this.BTCAmount;
    }

    public ECKey getOwner() {
        return this.ownerPublicKey;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + (this.BTCAmount == null ? 0 : this.BTCAmount.hashCode());
        result = prime * result
                + (this.ownerPublicKey == null ? 0 : this.ownerPublicKey.hashCode());
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
        final TransactionSlot other = (TransactionSlot) obj;
        if (this.BTCAmount == null) {
            if (other.BTCAmount != null) {
                return false;
            }
        } else if (!this.BTCAmount.equals(other.BTCAmount)) {
            return false;
        }
        if (this.ownerPublicKey == null) {
            if (other.ownerPublicKey != null) {
                return false;
            }
        } else if (!this.ownerPublicKey.equals(other.ownerPublicKey)) {
            return false;
        }
        return true;
    }
}
