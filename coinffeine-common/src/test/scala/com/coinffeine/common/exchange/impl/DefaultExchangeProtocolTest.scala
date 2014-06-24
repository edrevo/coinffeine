package com.coinffeine.common.exchange.impl

import com.coinffeine.common.Currency.Bitcoin
import com.coinffeine.common.Currency.Implicits._
import com.coinffeine.common.exchange.{BuyerRole, UnspentOutput}

class DefaultExchangeProtocolTest extends ExchangeTest {

  "An exchange protocol" should
    "start a handshake with a deposit of the right amount for the buyer" in new BuyerHandshake {
      val deposit = buyerHandshake.myDeposit.get
      Bitcoin.fromSatoshi(deposit.getValue(buyerWallet)) should be (-0.2.BTC)
      sendToBlockChain(deposit)
    }

  it should "start a handshake with a deposit of the right amount for the seller" in
    new SellerHandshake {
      val deposit = sellerHandshake.myDeposit.get
      Bitcoin.fromSatoshi(deposit.getValue(sellerWallet)) should be (-1.1.BTC)
      sendToBlockChain(deposit)
    }

  it should "require the unspent outputs to have a minimum amount" in new FreshInstance {
    val buyerWallet = createWallet(exchange.buyer.bitcoinKey, 0.1.BTC)
    val funds = UnspentOutput.collect(0.1.BTC, buyerWallet)
    an [IllegalArgumentException] should be thrownBy {
      protocol.createHandshake(exchange, BuyerRole, funds, buyerWallet.getChangeAddress)
    }
  }
}
