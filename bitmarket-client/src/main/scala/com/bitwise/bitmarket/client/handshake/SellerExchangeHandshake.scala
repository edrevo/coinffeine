package com.bitwise.bitmarket.client.handshake

import com.google.bitcoin.core._

import com.bitwise.bitmarket.client.Exchange

class SellerExchangeHandshake(exchange: Exchange, userWallet: Wallet)
  extends DefaultExchangeHandshake(
    exchange,
    amountToCommit = exchange.exchangeAmount, // TODO: Did the seller need a deposit?
    userWallet)
