package com.coinffeine.client.handshake

import com.google.bitcoin.core._

import com.coinffeine.client.Exchange

class BuyerHandshake(exchange: Exchange, userWallet: Wallet)
  extends DefaultHandshake(
    exchange,
    amountToCommit = exchange.exchangeAmount * 2 / exchange.steps,
    userWallet)
