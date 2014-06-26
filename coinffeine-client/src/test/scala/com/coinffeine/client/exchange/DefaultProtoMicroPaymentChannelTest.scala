package com.coinffeine.client.exchange

import scala.collection.JavaConversions._
import scala.concurrent.duration._

import akka.actor.ActorSystem
import akka.util.{Timeout => AkkaTimeout}
import com.google.bitcoin.core.Transaction.SigHash

import com.coinffeine.client.SampleExchangeInfo
import com.coinffeine.common.{BitcoinjTest, Currency}
import com.coinffeine.common.Currency.Implicits._
import com.coinffeine.common.bitcoin.{ImmutableTransaction, MutableTransaction, MutableTransactionOutput}
import com.coinffeine.common.exchange.{BuyerRole, SellerRole, UnspentOutput}
import com.coinffeine.common.exchange.MicroPaymentChannel.StepSignatures
import com.coinffeine.common.exchange.impl.{DefaultExchangeProtocol, TransactionProcessor}

class DefaultProtoMicroPaymentChannelTest
  extends BitcoinjTest with SampleExchangeInfo with DefaultExchangeProtocol.Component {

  private trait WithBasicSetup {
    val actorSystem = ActorSystem("DefaultExchangeTest")
    implicit val actorTimeout = AkkaTimeout(5.seconds)

    lazy val sellerWallet = createWallet(sellerExchangeInfo.user.bitcoinKey, 200 BTC)
    lazy val sellerHandshake = exchangeProtocol.createHandshake(
      sellerExchangeInfo.exchange, SellerRole, UnspentOutput.collect(11.BTC, sellerWallet),
      sellerWallet.getChangeAddress
    )

    lazy val buyerWallet = createWallet(buyerExchangeInfo.user.bitcoinKey, 5 BTC)
    lazy val buyerHandshake = exchangeProtocol.createHandshake(
      buyerExchangeInfo.exchange, BuyerRole, UnspentOutput.collect(2.BTC, buyerWallet),
      buyerWallet.getChangeAddress
    )
  }

  private trait WithChannels extends WithBasicSetup {
    sendToBlockChain(sellerHandshake.myDeposit.get)
    sendToBlockChain(buyerHandshake.myDeposit.get)
    val sellerChannel = new DefaultProtoMicroPaymentChannel[Currency.Euro.type](
      exchange,
      SellerRole,
      sellerHandshake.myDeposit,
      buyerHandshake.myDeposit)
    val buyerChannel = new DefaultProtoMicroPaymentChannel[Currency.Euro.type](
      exchange,
      BuyerRole,
      sellerHandshake.myDeposit,
      buyerHandshake.myDeposit)
  }

  "The default exchange" should "fail if the seller commitment tx is not valid" in new WithBasicSetup {
    val invalidFundsCommitment = new MutableTransaction(sellerExchangeInfo.parameters.network)
    invalidFundsCommitment.addInput(sellerWallet.calculateAllSpendCandidates(true).head)
    invalidFundsCommitment.addOutput((5 BTC).asSatoshi, sellerWallet.getKeys.head)
    invalidFundsCommitment.signInputs(SigHash.ALL, sellerWallet)
    sendToBlockChain(buyerHandshake.myDeposit.get)
    sendToBlockChain(invalidFundsCommitment)
    an [IllegalArgumentException] should be thrownBy {
      new DefaultProtoMicroPaymentChannel[Currency.Euro.type](
        exchange,
        SellerRole,
        ImmutableTransaction(invalidFundsCommitment),
        buyerHandshake.myDeposit
      )
    }
  }

  it should "fail if the buyer commitment tx is not valid" in new WithBasicSetup {
    val invalidFundsCommitment = new MutableTransaction(buyerExchangeInfo.parameters.network)
    invalidFundsCommitment.addInput(buyerWallet.calculateAllSpendCandidates(true).head)
    invalidFundsCommitment.addOutput((5 BTC).asSatoshi, buyerWallet.getKeys.head)
    invalidFundsCommitment.signInputs(SigHash.ALL, buyerWallet)
    sendToBlockChain(sellerHandshake.myDeposit.get)
    sendToBlockChain(invalidFundsCommitment)
    an [IllegalArgumentException] should be thrownBy {
      new DefaultProtoMicroPaymentChannel[Currency.Euro.type](
        exchange,
        SellerRole,
        sellerHandshake.myDeposit,
        ImmutableTransaction(invalidFundsCommitment)
      )
    }
  }

  it should "have a final offer that splits the amount as expected" in new WithChannels {
    val finalOffer = sellerChannel.finalOffer
    addAmounts(finalOffer.getInputs.map(_.getConnectedOutput)) should
      be (addAmounts(finalOffer.getOutputs))
    addAmounts(finalOffer.getInputs.map(_.getConnectedOutput)) should
      be (sellerExchangeInfo.parameters.bitcoinAmount + (sellerExchangeInfo.btcStepAmount * 3))

    TransactionProcessor.setMultipleSignatures(
      finalOffer, index = 0, buyerChannel.finalSignatures.buyerDepositSignature, sellerChannel.finalSignatures.buyerDepositSignature)
    TransactionProcessor.setMultipleSignatures(
      finalOffer, index = 1, buyerChannel.finalSignatures.sellerDepositSignature, sellerChannel.finalSignatures.sellerDepositSignature)
    sendToBlockChain(finalOffer)

    Currency.Bitcoin.fromSatoshi(sellerWallet.getBalance) should be (
      200.BTC - sellerExchangeInfo.parameters.bitcoinAmount)
    Currency.Bitcoin.fromSatoshi(buyerWallet.getBalance) should be (
      5.BTC + sellerExchangeInfo.parameters.bitcoinAmount)
  }

  it should "validate the seller's final signature" in new WithChannels {
    val StepSignatures(signature0, signature1) = sellerChannel.finalSignatures
    buyerChannel.validateSellersFinalSignature(signature0, signature1) should be ('success)
    sellerChannel.validateSellersFinalSignature(signature0, signature1) should be ('success)
    buyerChannel.validateSellersFinalSignature(signature1, signature0) should be ('failure)

    val StepSignatures(buyerSignature0, buyerSignature1) = buyerChannel.finalSignatures
    buyerChannel.validateSellersFinalSignature(buyerSignature0, buyerSignature1) should be ('failure)
    sellerChannel.validateSellersFinalSignature(buyerSignature0, buyerSignature1) should be ('failure)
  }

  for (step <- 1 to sampleExchangeInfo.parameters.breakdown.intermediateSteps) {
    it should s"have an intermediate offer $step that splits the amount as expected" in new WithChannels {
      val stepOffer = sellerChannel.getOffer(step)
      addAmounts(stepOffer.getInputs.map(_.getConnectedOutput)) should
        be (addAmounts(stepOffer.getOutputs) + (sellerExchangeInfo.btcStepAmount * 3))
      addAmounts(stepOffer.getInputs.map(_.getConnectedOutput)) should
        be (sellerExchangeInfo.parameters.bitcoinAmount + (sellerExchangeInfo.btcStepAmount * 3))

      sendToBlockChain(buyerChannel.getSignedOffer(step, sellerChannel.signStep(step)))

      val expectedSellerBalance = (200 BTC) - sellerExchangeInfo.btcStepAmount * (step + 1)
      Currency.Bitcoin.fromSatoshi(sellerWallet.getBalance) should be (expectedSellerBalance)
      val expectedBuyerBalance = (5 BTC) + sellerExchangeInfo.btcStepAmount * (step - 2)
      Currency.Bitcoin.fromSatoshi(buyerWallet.getBalance) should be (expectedBuyerBalance)
    }

    it should s"validate the seller's signatures correctly for step $step" in new WithChannels {
      val StepSignatures(signature0, signature1) = sellerChannel.signStep(step)
      buyerChannel.validateSellersSignature(step, signature0, signature1) should be ('success)
      sellerChannel.validateSellersSignature(step, signature0, signature1) should be ('success)
      buyerChannel.validateSellersSignature(step, signature1, signature0) should be ('failure)
      val StepSignatures(buyerSignature0, buyerSignature1) = buyerChannel.signStep(step)
      buyerChannel.validateSellersSignature(step, buyerSignature0, buyerSignature1) should be ('failure)
      sellerChannel.validateSellersSignature(step, buyerSignature0, buyerSignature1) should be ('failure)
    }
  }

  private def addAmounts(outputs: Seq[MutableTransactionOutput]) =
    outputs.map(o => Currency.Bitcoin.fromSatoshi(o.getValue)).reduce(_ + _)
}
