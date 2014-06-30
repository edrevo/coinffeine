package com.coinffeine.client.exchange

import scala.concurrent.duration._

import akka.actor.ActorSystem
import akka.util.{Timeout => AkkaTimeout}

import com.coinffeine.client.SampleExchangeInfo
import com.coinffeine.common.{BitcoinjTest, Currency}
import com.coinffeine.common.Currency.Implicits._
import com.coinffeine.common.exchange._
import com.coinffeine.common.exchange.MicroPaymentChannel._
import com.coinffeine.common.exchange.impl.DefaultExchangeProtocol

class DefaultProtoMicroPaymentChannelTest
  extends BitcoinjTest with SampleExchangeInfo with DefaultExchangeProtocol.Component {

  val finalStep = FinalStep(exchange.parameters.breakdown)

  private trait WithBasicSetup {
    val actorSystem = ActorSystem("DefaultExchangeTest")
    implicit val actorTimeout = AkkaTimeout(5.seconds)

    val sellerWallet = createWallet(exchange.seller.bitcoinKey, 200 BTC)
    val sellerHandshake = exchangeProtocol.createHandshake(
      exchange, SellerRole, UnspentOutput.collect(11.BTC, sellerWallet),
      sellerWallet.getChangeAddress
    )

    val buyerWallet = createWallet(exchange.buyer.bitcoinKey, 5 BTC)
    val buyerHandshake = exchangeProtocol.createHandshake(
      exchange, BuyerRole, UnspentOutput.collect(2.BTC, buyerWallet),
      buyerWallet.getChangeAddress
    )
    val deposits = exchangeProtocol.validateDeposits(
      Both(buyerHandshake.myDeposit, sellerHandshake.myDeposit), exchange
    ).get
  }

  private trait WithChannels extends WithBasicSetup {
    sendToBlockChain(sellerHandshake.myDeposit.get)
    sendToBlockChain(buyerHandshake.myDeposit.get)
    val sellerChannel = new DefaultProtoMicroPaymentChannel(exchange, SellerRole, deposits)
    val buyerChannel = new DefaultProtoMicroPaymentChannel(exchange, BuyerRole, deposits)
  }

  "The default exchange" should "have a final offer that splits the amount as expected" in new WithChannels {
    val sellerSignatures = sellerChannel.signStepTransaction(finalStep)
    val finalOffer = buyerChannel.closingTransaction(finalStep, sellerSignatures).get
    sendToBlockChain(finalOffer)

    Currency.Bitcoin.fromSatoshi(sellerWallet.getBalance) should be (
      200.BTC - exchange.amounts.bitcoinAmount)
    Currency.Bitcoin.fromSatoshi(buyerWallet.getBalance) should be (
      5.BTC + exchange.amounts.bitcoinAmount)
  }

  it should "validate the seller's final signature" in new WithChannels {
    val signatures = sellerChannel.signStepTransaction(finalStep)
    buyerChannel.validateStepTransactionSignatures(finalStep, signatures) should be ('success)
    sellerChannel.validateStepTransactionSignatures(finalStep, signatures) should be ('success)
    buyerChannel.validateStepTransactionSignatures(finalStep, signatures.swap) should be ('failure)

    val buyerSignatures = buyerChannel.signStepTransaction(finalStep)
    buyerChannel.validateStepTransactionSignatures(finalStep, buyerSignatures) should be ('failure)
    sellerChannel.validateStepTransactionSignatures(finalStep, buyerSignatures) should be ('failure)
  }

  for (i <- 1 to exchange.parameters.breakdown.intermediateSteps) {
    val step = IntermediateStep(i, exchange.parameters.breakdown)

    it should s"have an $step offer that splits the amount as expected" in new WithChannels {
      val stepOffer =
        buyerChannel.closingTransaction(step, sellerChannel.signStepTransaction(step)).get
      sendToBlockChain(stepOffer)

      val expectedSellerBalance = (200 BTC) - exchange.amounts.stepFiatAmount * (i + 1)
      Currency.Bitcoin.fromSatoshi(sellerWallet.getBalance) should be (expectedSellerBalance)
      val expectedBuyerBalance = (5 BTC) + exchange.amounts.stepFiatAmount * (i - 2)
      Currency.Bitcoin.fromSatoshi(buyerWallet.getBalance) should be (expectedBuyerBalance)
    }

    it should s"validate the seller's signatures correctly for $step" in new WithChannels {
      val signatures = sellerChannel.signStepTransaction(step)
      buyerChannel.validateStepTransactionSignatures(step, signatures) should be ('success)
      sellerChannel.validateStepTransactionSignatures(step, signatures) should be ('success)
      buyerChannel.validateStepTransactionSignatures(step, signatures.swap) should be ('failure)

      val buyerSignatures = buyerChannel.signStepTransaction(step)
      buyerChannel.validateStepTransactionSignatures(step, buyerSignatures) should be ('failure)
      sellerChannel.validateStepTransactionSignatures(step, buyerSignatures) should be ('failure)
    }
  }
}
