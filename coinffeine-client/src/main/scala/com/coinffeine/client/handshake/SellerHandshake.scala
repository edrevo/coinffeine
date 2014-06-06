package com.coinffeine.client.handshake

import com.google.bitcoin.core._

import com.coinffeine.client.ExchangeInfo
import com.coinffeine.common.FiatCurrency

class SellerHandshake[C <: FiatCurrency](exchangeInfo: ExchangeInfo[C], userWallet: Wallet)
  extends DefaultHandshake(
    exchangeInfo,
    amountToCommit = exchangeInfo.btcExchangeAmount + exchangeInfo.btcStepAmount,
    userWallet)
