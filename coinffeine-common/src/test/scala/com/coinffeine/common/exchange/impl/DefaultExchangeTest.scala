package com.coinffeine.common.exchange.impl

import com.coinffeine.common.{Currency, BitcoinjTest}
import com.coinffeine.common.Currency.Implicits._
import com.coinffeine.common.exchange.{SellerRole, BuyerRole}

class DefaultExchangeTest extends BitcoinjTest {

  import com.coinffeine.common.exchange.impl.Samples.exchange

  "An exchange" should "start a new handshake as buyer" in {
    val wallet = createWallet(exchange.buyer.bitcoinKey, 0.3.BTC)
    val buyerDeposit = TransactionProcessor.createMultisignDeposit(
      wallet, 0.2.BTC, Seq(exchange.buyer.bitcoinKey, exchange.seller.bitcoinKey), network)
    val handshake = exchange.startHandshake(BuyerRole, buyerDeposit)
    handshake.myDeposit should be (buyerDeposit)
    Currency.Bitcoin.fromSatoshi(handshake.myRefund.getValueSentToMe(wallet)) should be (0.1.BTC)
  }

  it should "start a new handshake as seller" in {
    val wallet = createWallet(exchange.seller.bitcoinKey, 2.BTC)
    val sellerDeposit = TransactionProcessor.createMultisignDeposit(
      wallet, 1.1.BTC, Seq(exchange.buyer.bitcoinKey, exchange.seller.bitcoinKey), network)
    val handshake = exchange.startHandshake(SellerRole, sellerDeposit)
    handshake.myDeposit should be (sellerDeposit)
    Currency.Bitcoin.fromSatoshi(handshake.myRefund.getValueSentToMe(wallet)) should be (1.BTC)
  }
}
