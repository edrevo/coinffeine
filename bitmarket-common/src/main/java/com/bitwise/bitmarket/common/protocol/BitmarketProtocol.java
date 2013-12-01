package com.bitwise.bitmarket.common.protocol;

import com.bitwise.bitmarket.common.PeerConnection;

public interface BitmarketProtocol extends AutoCloseable {

    void setOfferListener(OfferListener listener);

    void setExchangeRequestListener(ExchangeRequestListener listener);

    void publish(Offer offer) throws BitmarketProtocolException, OfferRejectedException;

    void requestExchange(ExchangeRequest acceptance, PeerConnection recipient)
            throws BitmarketProtocolException, ExchangeRejectedException;
}
