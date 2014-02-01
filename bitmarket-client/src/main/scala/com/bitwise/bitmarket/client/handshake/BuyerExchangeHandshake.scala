package com.bitwise.bitmarket.client.handshake

import com.google.bitcoin.core._

import com.bitwise.bitmarket.client.Exchange

class BuyerExchangeHandshake(exchange: Exchange, inputFunds: Seq[TransactionOutput], userWallet: Wallet)
  extends DefaultExchangeHandshake(
    exchange,
    inputFunds,
    amountToCommit = exchange.exchangeAmount * 2 / exchange.steps,
    userWallet)
