package com.bitwise.bitmarket.common.protocol;

import com.bitwise.bitmarket.common.ExchangeRejectedException;

public interface ExchangeRequestListener {

    void onExchangeRequest(ExchangeRequest acceptance) throws ExchangeRejectedException;
}
