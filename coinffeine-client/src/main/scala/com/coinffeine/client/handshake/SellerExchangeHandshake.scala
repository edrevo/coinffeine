package com.coinffeine.client.handshake

import com.google.bitcoin.core._

import com.coinffeine.client.Exchange

class SellerExchangeHandshake(exchange: Exchange, userWallet: Wallet)
  extends DefaultExchangeHandshake(
    exchange,
    amountToCommit = exchange.exchangeAmount, // TODO: Did the seller need a deposit?
    userWallet)
