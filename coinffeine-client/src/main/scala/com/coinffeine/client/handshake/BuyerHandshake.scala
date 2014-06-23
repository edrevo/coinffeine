package com.coinffeine.client.handshake

import com.coinffeine.client.ExchangeInfo
import com.coinffeine.common.FiatCurrency
import com.coinffeine.common.bitcoin.Wallet

class BuyerHandshake[C <: FiatCurrency](exchange: ExchangeInfo[C], userWallet: Wallet)
  extends DefaultHandshake(
    exchange,
    amountToCommit = exchange.btcStepAmount * 2,
    userWallet)
