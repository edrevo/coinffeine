package com.bitwise.bitmarket.common.protocol;

public class OfferRejectedException extends Exception {

    public static enum Reason {
        BROADCAST_FAILED("broadcast server failed while accepting the offer");

        private String message;

        Reason(String message) { this.message = message;  }

        public String getMessage() {
            return this.message;
        }
    }

    private Reason reason;

    public OfferRejectedException(Reason reason) {
        super(String.format("offer publish was rejected by remote peer: %s", reason.getMessage()));
    }

    public Reason getReason() {
        return this.reason;
    }
}
