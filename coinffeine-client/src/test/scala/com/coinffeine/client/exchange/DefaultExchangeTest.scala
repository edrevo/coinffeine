package com.coinffeine.client.exchange

import scala.collection.JavaConversions._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

import com.google.bitcoin.core.{TransactionOutput, ECKey, Transaction}
import com.google.bitcoin.core.Transaction.SigHash
import com.google.bitcoin.script.ScriptBuilder
import org.scalatest.concurrent.PatienceConfiguration.Timeout
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.time.{Span, Seconds}

import com.coinffeine.client.BitcoinjTest
import com.coinffeine.client.handshake.{BuyerHandshake, SellerHandshake}
import com.coinffeine.client.paymentprocessor.MockPaymentProcessorFactory
import com.coinffeine.common.currency.Implicits._
import com.coinffeine.common.currency.{FiatAmount, BtcAmount}

class DefaultExchangeTest extends BitcoinjTest with ScalaFutures {

  private trait WithBasicSetup {
    val sellerExchangeInfo = sampleExchangeInfo.copy(
      userKey = new ECKey(),
      userFiatAddress = "seller",
      counterpartKey = new ECKey(),
      counterpartFiatAddress = "buyer"
    )
    lazy val sellerWallet = createWallet(sellerExchangeInfo.userKey, 200 BTC)
    lazy val sellerHandshake = new SellerHandshake(sellerExchangeInfo, sellerWallet)
    val paymentProcFactory = new MockPaymentProcessorFactory()
    val sellerPaymentProc = paymentProcFactory.newProcessor(
      sellerExchangeInfo.userFiatAddress, Seq(0 EUR))

    val buyerExchangeInfo = sampleExchangeInfo.copy(
      userKey = sellerExchangeInfo.counterpartKey,
      userFiatAddress = sellerExchangeInfo.counterpartFiatAddress,
      counterpartKey = sellerExchangeInfo.userKey,
      counterpartFiatAddress = sellerExchangeInfo.userFiatAddress
    )
    lazy val buyerWallet = createWallet(buyerExchangeInfo.userKey, 5 BTC)
    lazy val buyerHandshake = new BuyerHandshake(buyerExchangeInfo, buyerWallet)
    val buyerPaymentProc = paymentProcFactory.newProcessor(
      buyerExchangeInfo.userFiatAddress, Seq(1000 EUR))
  }

  private trait WithExchange extends WithBasicSetup {
    sendToBlockChain(sellerHandshake.commitmentTransaction)
    sendToBlockChain(buyerHandshake.commitmentTransaction)
    val sellerExchange = new DefaultExchange(
      sellerExchangeInfo,
      sellerPaymentProc,
      sellerHandshake.commitmentTransaction,
      buyerHandshake.commitmentTransaction) with SellerUser
    val buyerExchange = new DefaultExchange(
      buyerExchangeInfo,
      buyerPaymentProc,
      sellerHandshake.commitmentTransaction,
      buyerHandshake.commitmentTransaction) with BuyerUser
  }

  "The default exchange" should "fail if the seller commitment tx is not valid" in new WithBasicSetup {
    val invalidFundsCommitment = new Transaction(sellerExchangeInfo.network)
    invalidFundsCommitment.addInput(sellerWallet.calculateAllSpendCandidates(true).head)
    invalidFundsCommitment.addOutput((5 BTC).asSatoshi, sellerWallet.getKeys.head)
    invalidFundsCommitment.signInputs(SigHash.ALL, sellerWallet)
    sendToBlockChain(buyerHandshake.commitmentTransaction)
    sendToBlockChain(invalidFundsCommitment)
    an [IllegalArgumentException] should be thrownBy { new DefaultExchange(
      sellerExchangeInfo,
      sellerPaymentProc,
      invalidFundsCommitment,
      buyerHandshake.commitmentTransaction) with SellerUser }
  }

  it should "fail if the buyer commitment tx is not valid" in new WithBasicSetup {
    val invalidFundsCommitment = new Transaction(buyerExchangeInfo.network)
    invalidFundsCommitment.addInput(buyerWallet.calculateAllSpendCandidates(true).head)
    invalidFundsCommitment.addOutput((5 BTC).asSatoshi, buyerWallet.getKeys.head)
    invalidFundsCommitment.signInputs(SigHash.ALL, buyerWallet)
    sendToBlockChain(sellerHandshake.commitmentTransaction)
    sendToBlockChain(invalidFundsCommitment)
    an [IllegalArgumentException] should be thrownBy { new DefaultExchange(
      sellerExchangeInfo,
      sellerPaymentProc,
      sellerHandshake.commitmentTransaction,
      invalidFundsCommitment) with SellerUser }
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
    BtcAmount(sellerWallet.getBalance) should be ((200 BTC) - sellerExchangeInfo.btcExchangeAmount)
    BtcAmount(buyerWallet.getBalance) should be ((5 BTC) + sellerExchangeInfo.btcExchangeAmount)
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
        (addAmounts(stepOffer.getOutputs) - (sellerExchangeInfo.btcStepAmount * 3))
      addAmounts(stepOffer.getInputs.map(_.getConnectedOutput)) should be
        sellerExchangeInfo.btcExchangeAmount + (sellerExchangeInfo.btcStepAmount * 3)
      stepOffer.getInput(0).setScriptSig(
        ScriptBuilder.createMultiSigInputScript(
          buyerExchange.signStep(step)._1, sellerExchange.signStep(step)._1))
      stepOffer.getInput(1).setScriptSig(
        ScriptBuilder.createMultiSigInputScript(
          sellerExchange.signStep(step)._2, buyerExchange.signStep(step)._2))
      sendToBlockChain(stepOffer)
      val expectedSellerBalance = (200 BTC) - sellerExchangeInfo.btcStepAmount * (step + 1)
      BtcAmount(sellerWallet.getBalance) should be (expectedSellerBalance)
      val expectedBuyerBalance = (5 BTC) + sellerExchangeInfo.btcStepAmount * (step - 2)
      BtcAmount(buyerWallet.getBalance) should be (expectedBuyerBalance)
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
          validation.futureValue should be ()
        else
          validation.map(_ => false).recover {
            case _: IllegalArgumentException => true
          }.futureValue should be (true)
      }
    }
  }

  it should "transfer the expected fiat amount" in new WithExchange {
    val balances = for {
        prevBuyerBalance <- buyerPaymentProc.currentBalance()
        prevSellerBalance <- sellerPaymentProc.currentBalance()
        payments <- Future.traverse(1 to buyerExchangeInfo.steps)(buyerExchange.pay)
        afterBuyerBalance <- buyerPaymentProc.currentBalance()
        afterSellerBalance <- sellerPaymentProc.currentBalance()
    } yield (prevBuyerBalance, afterBuyerBalance, prevSellerBalance, afterSellerBalance)
    whenReady(balances, Timeout(Span(30, Seconds))) { balances =>
      val prevBuyerBalance = balances._1
      val afterBuyerBalance = balances._2
      val prevSellerBalance = balances._3
      val afterSellerBalance = balances._4
      val exchangeCurrency = buyerExchangeInfo.fiatExchangeAmount.currency

      def notRelevant(balances: Seq[FiatAmount]) = balances.filterNot(_.currency == exchangeCurrency)
      def relevant(balances: Seq[FiatAmount]) = balances.find(_.currency == exchangeCurrency).get

      notRelevant(prevBuyerBalance) should be (notRelevant(afterBuyerBalance))
      relevant(prevBuyerBalance) should be (relevant(afterBuyerBalance) + buyerExchangeInfo.fiatExchangeAmount)

      notRelevant(prevSellerBalance) should be (notRelevant(afterSellerBalance))
      relevant(prevSellerBalance) should be (relevant(afterSellerBalance) - buyerExchangeInfo.fiatExchangeAmount)
    }
  }

  private def addAmounts(outputs: Seq[TransactionOutput]) =
    outputs.map(o => BtcAmount(o.getValue)).sum
}
