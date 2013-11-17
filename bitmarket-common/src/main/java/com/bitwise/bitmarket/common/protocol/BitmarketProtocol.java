package com.bitwise.bitmarket.common.protocol;

public interface BitmarketProtocol {

    void setOfferListener(OfferListener listener);

    void publish(Offer offer) throws BitmarketProtocolException;
}
