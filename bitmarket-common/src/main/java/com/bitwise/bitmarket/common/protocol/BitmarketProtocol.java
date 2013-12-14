package com.bitwise.bitmarket.common.protocol;

import java.util.concurrent.Future;

import com.bitwise.bitmarket.common.PeerConnection;

public interface BitmarketProtocol extends AutoCloseable {

    void setOfferListener(OfferListener listener);

    void setExchangeRequestListener(ExchangeRequestListener listener);

    Future<Void> publish(Offer offer) throws BitmarketProtocolException;

    Future<Void> requestExchange(
            ExchangeRequest acceptance,
            PeerConnection recipient) throws BitmarketProtocolException;
}
