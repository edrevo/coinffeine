package com.bitwise.bitmarket.common.protocol;

public class PeerId {

    private final String address;

    public PeerId(String address) { this.address = address; }

    @Override
    public String toString() {
        return this.address;
    }

    public String getAddress() {
        return this.address;
    }
}
