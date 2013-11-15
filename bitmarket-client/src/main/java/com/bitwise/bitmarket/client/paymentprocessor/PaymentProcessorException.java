package com.bitwise.bitmarket.client.paymentprocessor;

public class PaymentProcessorException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public PaymentProcessorException(String message) {
        super(message);
    }

    public PaymentProcessorException(Throwable cause) {
        super(cause);
    }

    public PaymentProcessorException(String message, Throwable cause) {
        super(message, cause);
    }
}
