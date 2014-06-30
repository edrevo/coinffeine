package com.coinffeine.client.exchange

import scala.collection.JavaConversions._
import scala.concurrent.duration._

import akka.actor.ActorSystem
import akka.util.{Timeout => AkkaTimeout}
import com.google.bitcoin.core.Transaction.SigHash

import com.coinffeine.client.SampleExchangeInfo
import com.coinffeine.common.{BitcoinjTest, Currency}
import com.coinffeine.common.Currency.Implicits._
import com.coinffeine.common.bitcoin.{ImmutableTransaction, MutableTransaction}
import com.coinffeine.common.exchange.{BuyerRole, Deposits, SellerRole, UnspentOutput}
import com.coinffeine.common.exchange.MicroPaymentChannel._
import com.coinffeine.common.exchange.impl.DefaultExchangeProtocol

class DefaultProtoMicroPaymentChannelTest
  extends BitcoinjTest with SampleExchangeInfo with DefaultExchangeProtocol.Component {

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
    val deposits = Deposits(buyerHandshake.myDeposit, sellerHandshake.myDeposit)
  }

  private trait WithChannels extends WithBasicSetup {
    sendToBlockChain(sellerHandshake.myDeposit.get)
    sendToBlockChain(buyerHandshake.myDeposit.get)
    val sellerChannel = new DefaultProtoMicroPaymentChannel(exchange, SellerRole, deposits)
    val buyerChannel = new DefaultProtoMicroPaymentChannel(exchange, BuyerRole, deposits)
  }

  "The default exchange" should "fail if the seller commitment tx is not valid" in new WithBasicSetup {
    val invalidFundsCommitment = new MutableTransaction(exchange.parameters.network)
    invalidFundsCommitment.addInput(sellerWallet.calculateAllSpendCandidates(true).head)
    invalidFundsCommitment.addOutput((5 BTC).asSatoshi, sellerWallet.getKeys.head)
    invalidFundsCommitment.signInputs(SigHash.ALL, sellerWallet)
    sendToBlockChain(buyerHandshake.myDeposit.get)
    sendToBlockChain(invalidFundsCommitment)
    an [IllegalArgumentException] should be thrownBy new DefaultProtoMicroPaymentChannel(
      exchange,
      SellerRole,
      deposits.copy(seller = ImmutableTransaction(invalidFundsCommitment))
    )
  }

  it should "fail if the buyer commitment tx is not valid" in new WithBasicSetup {
    val invalidFundsCommitment = new MutableTransaction(exchange.parameters.network)
    invalidFundsCommitment.addInput(buyerWallet.calculateAllSpendCandidates(true).head)
    invalidFundsCommitment.addOutput((5 BTC).asSatoshi, buyerWallet.getKeys.head)
    invalidFundsCommitment.signInputs(SigHash.ALL, buyerWallet)
    sendToBlockChain(sellerHandshake.myDeposit.get)
    sendToBlockChain(invalidFundsCommitment)
    an [IllegalArgumentException] should be thrownBy new DefaultProtoMicroPaymentChannel(
      exchange,
      SellerRole,
      deposits.copy(buyer = ImmutableTransaction(invalidFundsCommitment))
    )
  }

  it should "have a final offer that splits the amount as expected" in new WithChannels {
    val sellerSignatures = sellerChannel.signStepTransaction(FinalStep)
    val finalOffer = buyerChannel.closingTransaction(FinalStep, sellerSignatures).get
    sendToBlockChain(finalOffer)

    Currency.Bitcoin.fromSatoshi(sellerWallet.getBalance) should be (
      200.BTC - exchange.amounts.bitcoinAmount)
    Currency.Bitcoin.fromSatoshi(buyerWallet.getBalance) should be (
      5.BTC + exchange.amounts.bitcoinAmount)
  }

  it should "validate the seller's final signature" in new WithChannels {
    val signatures = sellerChannel.signStepTransaction(FinalStep)
    buyerChannel.validateStepTransactionSignatures(FinalStep, signatures) should be ('success)
    sellerChannel.validateStepTransactionSignatures(FinalStep, signatures) should be ('success)
    buyerChannel.validateStepTransactionSignatures(FinalStep, signatures.swap) should be ('failure)

    val buyerSignatures = buyerChannel.signStepTransaction(FinalStep)
    buyerChannel.validateStepTransactionSignatures(FinalStep, buyerSignatures) should be ('failure)
    sellerChannel.validateStepTransactionSignatures(FinalStep, buyerSignatures) should be ('failure)
  }

  for (i <- 1 to exchange.parameters.breakdown.intermediateSteps) {
    val step = IntermediateStep(i)

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
