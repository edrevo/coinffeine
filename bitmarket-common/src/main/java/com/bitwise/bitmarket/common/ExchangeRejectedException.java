package com.bitwise.bitmarket.common;

public class ExchangeRejectedException extends Exception {

    public static enum Reason {
        INVALID_AMOUNT("an invalid amount was requested to exchange");

        private String message;

        Reason(String message) { this.message = message;  }

        public String getMessage() {
            return this.message;
        }
    }

    private Reason reason;

    public ExchangeRejectedException(Reason reason) {
        super(String.format(
                "exchange request was rejected by remote peer: %s",
                reason.getMessage()));
    }

    public Reason getReason() {
        return this.reason;
    }
}
