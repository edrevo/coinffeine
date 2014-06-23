package com.coinffeine.client.exchange

import scala.collection.JavaConversions._
import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._

import akka.actor.ActorSystem
import akka.pattern._
import akka.util.{Timeout => AkkaTimeout}
import com.google.bitcoin.core.{ECKey, Transaction, TransactionOutput}
import com.google.bitcoin.core.Transaction.SigHash
import com.google.bitcoin.script.ScriptBuilder
import org.scalatest.concurrent.PatienceConfiguration.Timeout
import org.scalatest.time.{Seconds, Span}

import com.coinffeine.client.{ExchangeInfo, SampleExchangeInfo}
import com.coinffeine.client.handshake.{BuyerHandshake, SellerHandshake}
import com.coinffeine.client.paymentprocessor.MockPaymentProcessorFactory
import com.coinffeine.common.{BitcoinjTest, Currency}
import com.coinffeine.common.Currency.Implicits._
import com.coinffeine.common.paymentprocessor.PaymentProcessor

class DefaultExchangeTest extends BitcoinjTest with SampleExchangeInfo {

  private trait WithBasicSetup {
    val actorSystem = ActorSystem("DefaultExchangeTest")
    implicit val actorTimeout = AkkaTimeout(5.seconds)
    val sellerExchangeInfo: ExchangeInfo[Currency.Euro.type] = sampleExchangeInfo.copy(
      userKey = new ECKey(),
      userFiatAddress = "seller",
      counterpartKey = new ECKey(),
      counterpartFiatAddress = "buyer"
    )
    lazy val sellerWallet = createWallet(sellerExchangeInfo.userKey, 200 BTC)
    lazy val sellerHandshake = new SellerHandshake(sellerExchangeInfo, sellerWallet)
    val paymentProcFactory = new MockPaymentProcessorFactory()
    val sellerPaymentProc = actorSystem.actorOf(paymentProcFactory.newProcessor(
      sellerExchangeInfo.userFiatAddress, Seq(0 EUR)))

    val buyerExchangeInfo: ExchangeInfo[Currency.Euro.type] = sampleExchangeInfo.copy(
      userKey = sellerExchangeInfo.counterpartKey,
      userFiatAddress = sellerExchangeInfo.counterpartFiatAddress,
      counterpartKey = sellerExchangeInfo.userKey,
      counterpartFiatAddress = sellerExchangeInfo.userFiatAddress
    )
    lazy val buyerWallet = createWallet(buyerExchangeInfo.userKey, 5 BTC)
    lazy val buyerHandshake = new BuyerHandshake(buyerExchangeInfo, buyerWallet)
    val buyerPaymentProc = actorSystem.actorOf(paymentProcFactory.newProcessor(
      buyerExchangeInfo.userFiatAddress, Seq(1000 EUR)))
  }

  private trait WithExchange extends WithBasicSetup {
    sendToBlockChain(sellerHandshake.commitmentTransaction)
    sendToBlockChain(buyerHandshake.commitmentTransaction)
    val sellerExchange = new DefaultExchange[Currency.Euro.type](
      sellerExchangeInfo,
      sellerPaymentProc,
      sellerHandshake.commitmentTransaction,
      buyerHandshake.commitmentTransaction) with SellerUser[Currency.Euro.type]
    val buyerExchange = new DefaultExchange[Currency.Euro.type](
      buyerExchangeInfo,
      buyerPaymentProc,
      sellerHandshake.commitmentTransaction,
      buyerHandshake.commitmentTransaction) with BuyerUser[Currency.Euro.type]
  }

  "The default exchange" should "fail if the seller commitment tx is not valid" in new WithBasicSetup {
    val invalidFundsCommitment = new Transaction(sellerExchangeInfo.network)
    invalidFundsCommitment.addInput(sellerWallet.calculateAllSpendCandidates(true).head)
    invalidFundsCommitment.addOutput((5 BTC).asSatoshi, sellerWallet.getKeys.head)
    invalidFundsCommitment.signInputs(SigHash.ALL, sellerWallet)
    sendToBlockChain(buyerHandshake.commitmentTransaction)
    sendToBlockChain(invalidFundsCommitment)
    an [IllegalArgumentException] should be thrownBy { new DefaultExchange[Currency.Euro.type](
      sellerExchangeInfo,
      sellerPaymentProc,
      invalidFundsCommitment,
      buyerHandshake.commitmentTransaction) with SellerUser[Currency.Euro.type] }
  }

  it should "fail if the buyer commitment tx is not valid" in new WithBasicSetup {
    val invalidFundsCommitment = new Transaction(buyerExchangeInfo.network)
    invalidFundsCommitment.addInput(buyerWallet.calculateAllSpendCandidates(true).head)
    invalidFundsCommitment.addOutput((5 BTC).asSatoshi, buyerWallet.getKeys.head)
    invalidFundsCommitment.signInputs(SigHash.ALL, buyerWallet)
    sendToBlockChain(sellerHandshake.commitmentTransaction)
    sendToBlockChain(invalidFundsCommitment)
    an [IllegalArgumentException] should be thrownBy { new DefaultExchange[Currency.Euro.type](
      sellerExchangeInfo,
      sellerPaymentProc,
      sellerHandshake.commitmentTransaction,
      invalidFundsCommitment) with SellerUser[Currency.Euro.type] }
  }

  it should "have a final offer that splits the amount as expected" in new WithExchange {
    val finalOffer = sellerExchange.finalOffer
    addAmounts(finalOffer.getInputs.map(_.getConnectedOutput)) should be (addAmounts(finalOffer.getOutputs))
    addAmounts(finalOffer.getInputs.map(_.getConnectedOutput)) should be
      sellerExchangeInfo.btcExchangeAmount + (sellerExchangeInfo.btcStepAmount * 3)
    finalOffer.getInput(0).setScriptSig(
      ScriptBuilder.createMultiSigInputScript(
        buyerExchange.finalSignature._1, sellerExchange.finalSignature._1))
    finalOffer.getInput(1).setScriptSig(
      ScriptBuilder.createMultiSigInputScript(
        sellerExchange.finalSignature._2, buyerExchange.finalSignature._2))
    sendToBlockChain(finalOffer)
    Currency.Bitcoin.fromSatoshi(sellerWallet.getBalance) should be (
      (200 BTC) - sellerExchangeInfo.btcExchangeAmount)
    Currency.Bitcoin.fromSatoshi(buyerWallet.getBalance) should be (
      (5 BTC) + sellerExchangeInfo.btcExchangeAmount)
  }

  it should "validate the seller's final signature" in new WithExchange {
    val (signature0, signature1) = sellerExchange.finalSignature
    buyerExchange.validateSellersFinalSignature(signature0, signature1) should be ('success)
    sellerExchange.validateSellersFinalSignature(signature0, signature1) should be ('success)
    buyerExchange.validateSellersFinalSignature(signature1, signature0) should be ('failure)
    val (buyerSignature0, buyerSignature1) = buyerExchange.finalSignature
    buyerExchange.validateSellersFinalSignature(buyerSignature0, buyerSignature1) should be ('failure)
    sellerExchange.validateSellersFinalSignature(buyerSignature0, buyerSignature1) should be ('failure)
  }

  for (step <- 1 to sampleExchangeInfo.steps) {
    it should s"have an intermediate offer $step that splits the amount as expected" in new WithExchange {
      val stepOffer = sellerExchange.getOffer(step)
      addAmounts(stepOffer.getInputs.map(_.getConnectedOutput)) should be
        addAmounts(stepOffer.getOutputs) - (sellerExchangeInfo.btcStepAmount * 3)
      addAmounts(stepOffer.getInputs.map(_.getConnectedOutput)) should be
        sellerExchangeInfo.btcExchangeAmount + (sellerExchangeInfo.btcStepAmount * 3)
      val signedStepOffer = sellerExchange.getSignedOffer(step, buyerExchange.signStep(step))
      sendToBlockChain(signedStepOffer)
      val expectedSellerBalance = (200 BTC) - sellerExchangeInfo.btcStepAmount * (step + 1)
      Currency.Bitcoin.fromSatoshi(sellerWallet.getBalance) should be (expectedSellerBalance)
      val expectedBuyerBalance = (5 BTC) + sellerExchangeInfo.btcStepAmount * (step - 2)
      Currency.Bitcoin.fromSatoshi(buyerWallet.getBalance) should be (expectedBuyerBalance)
    }

    it should s"validate the seller's signatures correctly for step $step" in new WithExchange {
      val (signature0, signature1) = sellerExchange.signStep(step)
      buyerExchange.validateSellersSignature(step, signature0, signature1) should be ('success)
      sellerExchange.validateSellersSignature(step, signature0, signature1) should be ('success)
      buyerExchange.validateSellersSignature(step, signature1, signature0) should be ('failure)
      val (buyerSignature0, buyerSignature1) = buyerExchange.signStep(step)
      buyerExchange.validateSellersSignature(step, buyerSignature0, buyerSignature1) should be ('failure)
      sellerExchange.validateSellersSignature(step, buyerSignature0, buyerSignature1) should be ('failure)
    }

    it should s"validate payments for step $step" in new WithExchange {
      for (currentStep <- 1 to buyerExchangeInfo.steps) {
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
        payments <- Future.traverse(1 to buyerExchangeInfo.steps)(buyerExchange.pay)
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

      prevBuyerBalance should be (afterBuyerBalance + buyerExchangeInfo.fiatExchangeAmount)
      prevSellerBalance should be (afterSellerBalance - buyerExchangeInfo.fiatExchangeAmount)
    }
  }

  private def addAmounts(outputs: Seq[TransactionOutput]) =
    outputs.map(o => Currency.Bitcoin.fromSatoshi(o.getValue)).reduce(_ + _)
}
