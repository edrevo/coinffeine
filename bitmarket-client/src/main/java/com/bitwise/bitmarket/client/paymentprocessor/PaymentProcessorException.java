package com.bitwise.bitmarket.client.paymentprocessor;

public class PaymentProcessorException extends RuntimeException {

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
