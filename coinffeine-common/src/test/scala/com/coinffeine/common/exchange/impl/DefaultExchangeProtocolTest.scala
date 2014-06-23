package com.coinffeine.common.exchange.impl

import com.coinffeine.common.BitcoinjTest
import com.coinffeine.common.Currency.Bitcoin
import com.coinffeine.common.Currency.Implicits._
import com.coinffeine.common.exchange.{UnspentOutput, SellerRole, BuyerRole}

class DefaultExchangeProtocolTest extends BitcoinjTest {

  import Samples.exchange

  val protocol = new DefaultExchangeProtocol()

  "An exchange protocol" should
    "start a handshake with a deposit of the right amount for the buyer" in {
      val buyerWallet = createWallet(exchange.buyer.bitcoinKey, 1.BTC)
      val funds = UnspentOutput.collect(0.2.BTC, buyerWallet)
      val handshake =
        protocol.createHandshake(exchange, BuyerRole, funds, buyerWallet.getChangeAddress)
      val deposit = handshake.myDeposit.get

      Bitcoin.fromSatoshi(deposit.getValue(buyerWallet)) should be (-0.2.BTC)
      sendToBlockChain(deposit)
    }

  it should "start a handshake with a deposit of the right amount for the seller" in {
    val sellerWallet = createWallet(exchange.seller.bitcoinKey, 2.BTC)
    val funds = UnspentOutput.collect(1.1.BTC, sellerWallet)
    val handshake =
      protocol.createHandshake(exchange, SellerRole, funds, sellerWallet.getChangeAddress)
    val deposit = handshake.myDeposit.get
    Bitcoin.fromSatoshi(deposit.getValue(sellerWallet)) should be (-1.1.BTC)
    sendToBlockChain(deposit)
  }

  it should "require the unspent outputs to have a minimum amount" in {
    val buyerWallet = createWallet(exchange.buyer.bitcoinKey, 0.1.BTC)
    val funds = UnspentOutput.collect(0.1.BTC, buyerWallet)
    an [IllegalArgumentException] should be thrownBy {
      protocol.createHandshake(exchange, BuyerRole, funds, buyerWallet.getChangeAddress)
    }
  }
}
