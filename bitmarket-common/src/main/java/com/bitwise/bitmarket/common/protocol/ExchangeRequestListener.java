package com.bitwise.bitmarket.common.protocol;

public interface ExchangeRequestListener {

    void onExchangeRequest(ExchangeRequest acceptance) throws ExchangeRejectedException;
}
