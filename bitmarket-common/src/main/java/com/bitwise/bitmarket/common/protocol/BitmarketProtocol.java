package com.bitwise.bitmarket.common.protocol;

import com.bitwise.bitmarket.common.ExchangeRejectedException;
import com.bitwise.bitmarket.common.PeerConnection;

public interface BitmarketProtocol {

    void setOfferListener(OfferListener listener);

    void setExchangeRequestListener(ExchangeRequestListener listener);

    void publish(Offer offer) throws BitmarketProtocolException;

    void requestExchange(ExchangeRequest acceptance, PeerConnection recipient)
            throws BitmarketProtocolException, ExchangeRejectedException;
}
