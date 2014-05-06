package com.coinffeine.client.exchange

import scala.collection.JavaConversions._

import com.google.bitcoin.core.{TransactionOutput, ECKey, Transaction}
import com.google.bitcoin.core.Transaction.SigHash

import com.coinffeine.client.BitcoinjTest
import com.coinffeine.client.handshake.{BuyerHandshake, SellerHandshake}
import com.coinffeine.client.paymentprocessor.MockPaymentProcessor
import com.coinffeine.common.currency.Implicits._
import com.coinffeine.common.currency.BtcAmount
import com.google.bitcoin.script.ScriptBuilder

class DefaultExchangeTest extends BitcoinjTest {

  private trait WithBasicSetup {
    val sellerExchangeInfo = sampleExchangeInfo.copy(
      userKey = new ECKey(),
      userFiatAddress = "seller",
      counterpartKey = new ECKey(),
      counterpartFiatAddress = "buyer"
    )
    lazy val sellerWallet = createWallet(sellerExchangeInfo.userKey, 200 BTC)
    lazy val sellerHandshake = new SellerHandshake(sellerExchangeInfo, sellerWallet)
    val sellerPaymentProc = new MockPaymentProcessor(sellerExchangeInfo.userFiatAddress, Seq(0 EUR))

    val buyerExchangeInfo = sampleExchangeInfo.copy(
      userKey = sellerExchangeInfo.counterpartKey,
      userFiatAddress = sellerExchangeInfo.counterpartFiatAddress,
      counterpartKey = sellerExchangeInfo.userKey,
      counterpartFiatAddress = sellerExchangeInfo.userFiatAddress
    )
    lazy val buyerWallet = createWallet(buyerExchangeInfo.userKey, 5 BTC)
    lazy val buyerHandshake = new BuyerHandshake(buyerExchangeInfo, buyerWallet)
    val buyerPaymentProc = new MockPaymentProcessor(
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
    def addAmounts(outputs: Seq[TransactionOutput]) = outputs.map(o => BtcAmount(o.getValue)).sum
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
}
