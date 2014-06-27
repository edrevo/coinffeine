package com.coinffeine.common.exchange.impl

import com.coinffeine.common.{BitcoinAmount, BitcoinjTest}
import com.coinffeine.common.Currency.Bitcoin
import com.coinffeine.common.Currency.Implicits._
import com.coinffeine.common.bitcoin.{ImmutableTransaction, Wallet}
import com.coinffeine.common.exchange.{BuyerRole, Deposits, SellerRole, UnspentOutput}

/** Base trait for testing the default exchange protocol */
trait ExchangeTest extends BitcoinjTest {

  def balance(wallet: Wallet): BitcoinAmount = Bitcoin.fromSatoshi(wallet.getBalance)

  def valueSent(tx: ImmutableTransaction, wallet: Wallet): BitcoinAmount =
    Bitcoin.fromSatoshi(tx.get.getValueSentToMe(wallet))

  /** Fixture with just a fresh protocol object */
  trait FreshInstance {
    val exchange = Samples.exchange
    val protocol = new DefaultExchangeProtocol()
  }

  /** Fixture with a buyer handshake with the right amount of funds */
  trait BuyerHandshake extends FreshInstance {
    val buyerWallet = createWallet(exchange.buyer.bitcoinKey, 0.2.BTC)
    val buyerFunds = UnspentOutput.collect(0.2.BTC, buyerWallet)
    val buyerHandshake =
      protocol.createHandshake(exchange, BuyerRole, buyerFunds, buyerWallet.getChangeAddress)
  }

  /** Fixture with a seller handshake with the right amount of funds */
  trait SellerHandshake extends FreshInstance {
    val sellerWallet = createWallet(exchange.seller.bitcoinKey, 1.1.BTC)
    val sellerFunds = UnspentOutput.collect(1.1.BTC, sellerWallet)
    val sellerHandshake =
      protocol.createHandshake(exchange, SellerRole, sellerFunds, sellerWallet.getChangeAddress)
  }

  /** Fixture with buyer and seller channels created after a successful handshake */
  trait Channels extends BuyerHandshake with SellerHandshake {
    val deposits = Deposits(buyerHandshake.myDeposit, sellerHandshake.myDeposit)
    sendToBlockChain(deposits.toSeq.map(_.get): _*)
    val buyerChannel = protocol.createMicroPaymentChannel(exchange, BuyerRole, deposits)
    val sellerChannel = protocol.createMicroPaymentChannel(exchange, SellerRole, deposits)
    val totalSteps = exchange.parameters.breakdown.totalSteps
    val buyerChannels = Seq.iterate(buyerChannel, totalSteps)(_.nextStep)
    val sellerChannels = Seq.iterate(sellerChannel, totalSteps)(_.nextStep)
  }
}
