package com.bitwise.bitmarket.common.protocol;

public class BitmarketProtocolException extends Exception {

    public BitmarketProtocolException(String message) {
        super(message);
    }

    public BitmarketProtocolException(String message, Throwable cause) {
        super(message, cause);
    }

    public BitmarketProtocolException(Throwable cause) {
        super(cause);
    }
}
