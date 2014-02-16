package com.coinffeine.client.handshake

import com.google.bitcoin.core._

import com.coinffeine.client.Exchange

class BuyerExchangeHandshake(exchange: Exchange, userWallet: Wallet)
  extends DefaultExchangeHandshake(
    exchange,
    amountToCommit = exchange.exchangeAmount * 2 / exchange.steps,
    userWallet)
