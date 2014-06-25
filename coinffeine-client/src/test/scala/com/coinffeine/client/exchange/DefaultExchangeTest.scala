package com.coinffeine.client.exchange

import com.coinffeine.common.exchange.MicroPaymentChannel.StepSignatures

import scala.collection.JavaConversions._
import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._

import akka.actor.ActorSystem
import akka.pattern._
import akka.util.{Timeout => AkkaTimeout}
import com.google.bitcoin.core.Transaction.SigHash
import org.scalatest.concurrent.PatienceConfiguration.Timeout
import org.scalatest.time.{Seconds, Span}

import com.coinffeine.client.SampleExchangeInfo
import com.coinffeine.client.handshake.DefaultHandshake
import com.coinffeine.client.paymentprocessor.MockPaymentProcessorFactory
import com.coinffeine.common.{BitcoinjTest, Currency}
import com.coinffeine.common.Currency.Implicits._
import com.coinffeine.common.bitcoin.{ImmutableTransaction, MutableTransaction, MutableTransactionOutput}
import com.coinffeine.common.exchange.impl.TransactionProcessor
import com.coinffeine.common.paymentprocessor.PaymentProcessor

class DefaultExchangeTest extends BitcoinjTest with SampleExchangeInfo {

  private trait WithBasicSetup {
    val actorSystem = ActorSystem("DefaultExchangeTest")
    implicit val actorTimeout = AkkaTimeout(5.seconds)

    lazy val sellerWallet = createWallet(sellerExchangeInfo.user.bitcoinKey, 200 BTC)
    lazy val sellerHandshake = new DefaultHandshake(sellerExchangeInfo, sellerWallet)
    val paymentProcFactory = new MockPaymentProcessorFactory()
    val sellerPaymentProc = actorSystem.actorOf(paymentProcFactory.newProcessor(
      sellerExchangeInfo.user.paymentProcessorAccount, Seq(0 EUR)))

    lazy val buyerWallet = createWallet(buyerExchangeInfo.user.bitcoinKey, 5 BTC)
    lazy val buyerHandshake = new DefaultHandshake(buyerExchangeInfo, buyerWallet)
    val buyerPaymentProc = actorSystem.actorOf(paymentProcFactory.newProcessor(
      buyerExchangeInfo.user.paymentProcessorAccount, Seq(1000 EUR)))
  }

  private trait WithExchange extends WithBasicSetup {
    sendToBlockChain(sellerHandshake.myDeposit.get)
    sendToBlockChain(buyerHandshake.myDeposit.get)
    val sellerExchange = new DefaultExchange[Currency.Euro.type](
      sellerExchangeInfo,
      sellerPaymentProc,
      sellerHandshake.myDeposit,
      buyerHandshake.myDeposit) with SellerUser[Currency.Euro.type]
    val buyerExchange = new DefaultExchange[Currency.Euro.type](
      buyerExchangeInfo,
      buyerPaymentProc,
      sellerHandshake.myDeposit,
      buyerHandshake.myDeposit) with BuyerUser[Currency.Euro.type]
  }

  "The default exchange" should "fail if the seller commitment tx is not valid" in new WithBasicSetup {
    val invalidFundsCommitment = new MutableTransaction(sellerExchangeInfo.parameters.network)
    invalidFundsCommitment.addInput(sellerWallet.calculateAllSpendCandidates(true).head)
    invalidFundsCommitment.addOutput((5 BTC).asSatoshi, sellerWallet.getKeys.head)
    invalidFundsCommitment.signInputs(SigHash.ALL, sellerWallet)
    sendToBlockChain(buyerHandshake.myDeposit.get)
    sendToBlockChain(invalidFundsCommitment)
    an [IllegalArgumentException] should be thrownBy { new DefaultExchange[Currency.Euro.type](
      sellerExchangeInfo,
      sellerPaymentProc,
      ImmutableTransaction(invalidFundsCommitment),
      buyerHandshake.myDeposit) with SellerUser[Currency.Euro.type] }
  }

  it should "fail if the buyer commitment tx is not valid" in new WithBasicSetup {
    val invalidFundsCommitment = new MutableTransaction(buyerExchangeInfo.parameters.network)
    invalidFundsCommitment.addInput(buyerWallet.calculateAllSpendCandidates(true).head)
    invalidFundsCommitment.addOutput((5 BTC).asSatoshi, buyerWallet.getKeys.head)
    invalidFundsCommitment.signInputs(SigHash.ALL, buyerWallet)
    sendToBlockChain(sellerHandshake.myDeposit.get)
    sendToBlockChain(invalidFundsCommitment)
    an [IllegalArgumentException] should be thrownBy { new DefaultExchange[Currency.Euro.type](
      sellerExchangeInfo,
      sellerPaymentProc,
      sellerHandshake.myDeposit,
      ImmutableTransaction(invalidFundsCommitment)) with SellerUser[Currency.Euro.type] }
  }

  it should "have a final offer that splits the amount as expected" in new WithExchange {
    val finalOffer = sellerExchange.finalOffer
    addAmounts(finalOffer.getInputs.map(_.getConnectedOutput)) should
      be (addAmounts(finalOffer.getOutputs))
    addAmounts(finalOffer.getInputs.map(_.getConnectedOutput)) should
      be (sellerExchangeInfo.parameters.bitcoinAmount + (sellerExchangeInfo.btcStepAmount * 3))

    TransactionProcessor.setMultipleSignatures(
      finalOffer, index = 0, buyerExchange.finalSignatures.buyerDepositSignature, sellerExchange.finalSignatures.buyerDepositSignature)
    TransactionProcessor.setMultipleSignatures(
      finalOffer, index = 1, buyerExchange.finalSignatures.sellerDepositSignature, sellerExchange.finalSignatures.sellerDepositSignature)
    sendToBlockChain(finalOffer)

    Currency.Bitcoin.fromSatoshi(sellerWallet.getBalance) should be (
      200.BTC - sellerExchangeInfo.parameters.bitcoinAmount)
    Currency.Bitcoin.fromSatoshi(buyerWallet.getBalance) should be (
      5.BTC + sellerExchangeInfo.parameters.bitcoinAmount)
  }

  it should "validate the seller's final signature" in new WithExchange {
    val StepSignatures(signature0, signature1) = sellerExchange.finalSignatures
    buyerExchange.validateSellersFinalSignature(signature0, signature1) should be ('success)
    sellerExchange.validateSellersFinalSignature(signature0, signature1) should be ('success)
    buyerExchange.validateSellersFinalSignature(signature1, signature0) should be ('failure)

    val StepSignatures(buyerSignature0, buyerSignature1) = buyerExchange.finalSignatures
    buyerExchange.validateSellersFinalSignature(buyerSignature0, buyerSignature1) should be ('failure)
    sellerExchange.validateSellersFinalSignature(buyerSignature0, buyerSignature1) should be ('failure)
  }

  for (step <- 1 to sampleExchangeInfo.parameters.breakdown.intermediateSteps) {
    it should s"have an intermediate offer $step that splits the amount as expected" in new WithExchange {
      val stepOffer = sellerExchange.getOffer(step)
      addAmounts(stepOffer.getInputs.map(_.getConnectedOutput)) should
        be (addAmounts(stepOffer.getOutputs) + (sellerExchangeInfo.btcStepAmount * 3))
      addAmounts(stepOffer.getInputs.map(_.getConnectedOutput)) should
        be (sellerExchangeInfo.parameters.bitcoinAmount + (sellerExchangeInfo.btcStepAmount * 3))

      sendToBlockChain(buyerExchange.getSignedOffer(step, sellerExchange.signStep(step)))

      val expectedSellerBalance = (200 BTC) - sellerExchangeInfo.btcStepAmount * (step + 1)
      Currency.Bitcoin.fromSatoshi(sellerWallet.getBalance) should be (expectedSellerBalance)
      val expectedBuyerBalance = (5 BTC) + sellerExchangeInfo.btcStepAmount * (step - 2)
      Currency.Bitcoin.fromSatoshi(buyerWallet.getBalance) should be (expectedBuyerBalance)
    }

    it should s"validate the seller's signatures correctly for step $step" in new WithExchange {
      val StepSignatures(signature0, signature1) = sellerExchange.signStep(step)
      buyerExchange.validateSellersSignature(step, signature0, signature1) should be ('success)
      sellerExchange.validateSellersSignature(step, signature0, signature1) should be ('success)
      buyerExchange.validateSellersSignature(step, signature1, signature0) should be ('failure)
      val StepSignatures(buyerSignature0, buyerSignature1) = buyerExchange.signStep(step)
      buyerExchange.validateSellersSignature(step, buyerSignature0, buyerSignature1) should be ('failure)
      sellerExchange.validateSellersSignature(step, buyerSignature0, buyerSignature1) should be ('failure)
    }

    it should s"validate payments for step $step" in new WithExchange {
      for (currentStep <- 1 to buyerExchangeInfo.parameters.breakdown.intermediateSteps) {
        val validation = for {
          payment <- buyerExchange.pay(currentStep)
          _ <- sellerExchange.validatePayment(step, payment.id)
        } yield ()
        if (currentStep == step)
          validation.futureValue should be (())
        else
          validation.map(_ => false).recover {
            case _: IllegalArgumentException => true
          }.futureValue should be (true)
      }
    }
  }

  it should "transfer the expected fiat amount" in new WithExchange {
    val balances = for {
        prevBuyerBalance <- buyerPaymentProc.ask(
          PaymentProcessor.RetrieveBalance(Currency.Euro)).mapTo[PaymentProcessor.BalanceRetrieved[Currency.Euro.type]]
        prevSellerBalance <- sellerPaymentProc.ask(
          PaymentProcessor.RetrieveBalance(Currency.Euro)).mapTo[PaymentProcessor.BalanceRetrieved[Currency.Euro.type]]
        payments <- Future.traverse(1 to buyerExchangeInfo.parameters.breakdown.intermediateSteps)(buyerExchange.pay)
        afterBuyerBalance <- buyerPaymentProc.ask(
          PaymentProcessor.RetrieveBalance(Currency.Euro)).mapTo[PaymentProcessor.BalanceRetrieved[Currency.Euro.type]]
        afterSellerBalance <- sellerPaymentProc.ask(
          PaymentProcessor.RetrieveBalance(Currency.Euro)).mapTo[PaymentProcessor.BalanceRetrieved[Currency.Euro.type]]
    } yield (prevBuyerBalance.balance, afterBuyerBalance.balance,
        prevSellerBalance.balance, afterSellerBalance.balance)
    whenReady(balances, Timeout(Span(30, Seconds))) { balances =>
      val prevBuyerBalance = balances._1
      val afterBuyerBalance = balances._2
      val prevSellerBalance = balances._3
      val afterSellerBalance = balances._4

      prevBuyerBalance should be (afterBuyerBalance + buyerExchangeInfo.parameters.fiatAmount)
      prevSellerBalance should be (afterSellerBalance - buyerExchangeInfo.parameters.fiatAmount)
    }
  }

  private def addAmounts(outputs: Seq[MutableTransactionOutput]) =
    outputs.map(o => Currency.Bitcoin.fromSatoshi(o.getValue)).reduce(_ + _)
}
