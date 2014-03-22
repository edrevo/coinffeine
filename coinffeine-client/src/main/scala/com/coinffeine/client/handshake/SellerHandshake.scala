package com.coinffeine.client.handshake

import com.google.bitcoin.core._

import com.coinffeine.client.ExchangeInfo

class SellerHandshake(exchangeInfo: ExchangeInfo, userWallet: Wallet)
  extends DefaultHandshake(
    exchangeInfo,
    amountToCommit = exchangeInfo.btcExchangeAmount * (1 + 1 / exchangeInfo.steps),
    userWallet)
