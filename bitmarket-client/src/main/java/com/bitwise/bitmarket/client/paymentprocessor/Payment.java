package com.bitwise.bitmarket.client.paymentprocessor;

import com.bitwise.bitmarket.common.currency.FiatAmount;
import org.joda.time.DateTime;

public class Payment {

    private final String senderId;
    private final String receiverId;
    private final FiatAmount amount;
    private final DateTime date;
    private final String description;
    private final String id;

    public Payment(String senderId, String receiverId, FiatAmount amount,
            DateTime date, String description, String id) {
        this.senderId = senderId;
        this.receiverId = receiverId;
        this.amount = amount;
        this.date = date;
        this.description = description;
        this.id = id;
    }

    public String getSenderId() {
        return this.senderId;
    }

    public String getReceiverId() {
        return this.receiverId;
    }

    public FiatAmount getAmount() {
        return this.amount;
    }

    public DateTime getDate() {
        return this.date;
    }

    public String getDescription() {
        return this.description;
    }

    public String getId() {
        return this.id;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result
                + (this.amount == null ? 0 : this.amount.hashCode());
        result = prime * result
                + (this.date == null ? 0 : this.date.hashCode());
        result = prime * result
                + (this.description == null ? 0 : this.description.hashCode());
        result = prime * result + (this.id == null ? 0 : this.id.hashCode());
        result = prime * result
                + (this.receiverId == null ? 0 : this.receiverId.hashCode());
        result = prime * result
                + (this.senderId == null ? 0 : this.senderId.hashCode());
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
        Payment other = (Payment) obj;
        if (this.amount == null) {
            if (other.amount != null) {
                return false;
            }
        } else if (!this.amount.equals(other.amount)) {
            return false;
        }
        if (this.date == null) {
            if (other.date != null) {
                return false;
            }
        } else if (!this.date.equals(other.date)) {
            return false;
        }
        if (this.description == null) {
            if (other.description != null) {
                return false;
            }
        } else if (!this.description.equals(other.description)) {
            return false;
        }
        if (this.id == null) {
            if (other.id != null) {
                return false;
            }
        } else if (!this.id.equals(other.id)) {
            return false;
        }
        if (this.receiverId == null) {
            if (other.receiverId != null) {
                return false;
            }
        } else if (!this.receiverId.equals(other.receiverId)) {
            return false;
        }
        if (this.senderId == null) {
            if (other.senderId != null) {
                return false;
            }
        } else if (!this.senderId.equals(other.senderId)) {
            return false;
        }
        return true;
    }
}
