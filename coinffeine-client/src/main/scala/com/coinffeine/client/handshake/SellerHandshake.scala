package com.coinffeine.client.handshake

import com.google.bitcoin.core._

import com.coinffeine.client.Exchange

class SellerHandshake(exchange: Exchange, userWallet: Wallet)
  extends DefaultHandshake(
    exchange,
    amountToCommit = exchange.exchangeAmount * (1 + 1 / exchange.steps),
    userWallet)
