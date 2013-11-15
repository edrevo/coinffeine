package com.bitwise.bitmarket.common.protocol;

public interface BitmarketProtocol {

    void publish(Offer offer) throws BitmarketProtocolException;
}
