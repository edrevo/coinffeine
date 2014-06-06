package com.coinffeine.client.handshake

import com.google.bitcoin.core._

import com.coinffeine.client.ExchangeInfo
import com.coinffeine.common.FiatCurrency

class BuyerHandshake[C <: FiatCurrency](exchange: ExchangeInfo[C], userWallet: Wallet)
  extends DefaultHandshake(
    exchange,
    amountToCommit = exchange.btcStepAmount * 2,
    userWallet)
