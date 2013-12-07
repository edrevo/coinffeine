package com.bitwise.bitmarket.common.bitcoin;

public class BitcoinException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public BitcoinException(String message) {
        super(message);
    }

    public BitcoinException(Throwable cause) {
        super(cause);
    }

    public BitcoinException(String message, Throwable cause) {
        super(message, cause);
    }
}
