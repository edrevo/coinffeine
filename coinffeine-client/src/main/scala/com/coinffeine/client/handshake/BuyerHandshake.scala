package com.coinffeine.client.handshake

import com.google.bitcoin.core._

import com.coinffeine.client.ExchangeInfo

class BuyerHandshake(exchange: ExchangeInfo, userWallet: Wallet)
  extends DefaultHandshake(
    exchange,
    amountToCommit = exchange.btcExchangeAmount * 2 / exchange.steps,
    userWallet)
