package com.bitwise.bitmarket.common.protocol;

public class OfferId {

    private final int bytes;

    public OfferId(int bytes) { this.bytes = bytes; }

    @Override
    public String toString() { return Integer.toHexString(bytes); }

    public int getBytes() {
        return bytes;
    }
}
