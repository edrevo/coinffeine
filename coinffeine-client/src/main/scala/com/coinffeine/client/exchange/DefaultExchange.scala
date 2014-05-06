package com.coinffeine.client.exchange

import scala.collection.JavaConversions._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.Try

import com.google.bitcoin.core.{TransactionOutput, Transaction}
import com.google.bitcoin.core.Transaction.SigHash
import com.google.bitcoin.crypto.TransactionSignature
import com.google.bitcoin.script.ScriptBuilder

import com.coinffeine.client.{MultiSigInfo, ExchangeInfo}
import com.coinffeine.common.paymentprocessor.{PaymentProcessor, Payment}
import com.coinffeine.common.currency.BtcAmount

class DefaultExchange(
    override val exchangeInfo: ExchangeInfo,
    paymentProcessor: PaymentProcessor,
    sellerCommitmentTx: Transaction,
    buyerCommitmentTx: Transaction) extends Exchange {
  this: UserRole =>

  private val sellerFunds = sellerCommitmentTx.getOutput(0)
  private val buyerFunds = buyerCommitmentTx.getOutput(0)
  requireValidBuyerFunds(buyerFunds)
  requireValidSellerFunds(sellerFunds)

  private def requireValidFunds(funds: TransactionOutput): Unit = {
    require(funds.getScriptPubKey.isSentToMultiSig,
      "Transaction with funds is invalid because is not sending the funds to a multisig")
    val multisigInfo = MultiSigInfo(funds.getScriptPubKey)
    require(multisigInfo.requiredKeyCount == 2,
      "Funds are sent to a multisig that do not require 2 keys")
    require(multisigInfo.possibleKeys == Set(exchangeInfo.userKey, exchangeInfo.counterpartKey),
      "Possible keys in multisig script does not match the expected keys")
  }

  private def requireValidBuyerFunds(buyerFunds: TransactionOutput): Unit = {
    requireValidFunds(buyerFunds)
    require(BtcAmount(buyerFunds.getValue) == exchangeInfo.btcStepAmount * 2,
      "The amount of committed funds by the buyer does not match the expected amount")
  }

  private def requireValidSellerFunds(sellerFunds: TransactionOutput): Unit = {
    requireValidFunds(sellerFunds)
    require(
      BtcAmount(sellerFunds.getValue) == exchangeInfo.btcExchangeAmount + exchangeInfo.btcStepAmount,
      "The amount of committed funds by the seller does not match the expected amount")
  }

  private val offers = for (step <- 1 to exchangeInfo.steps) yield {
    val tx = new Transaction(exchangeInfo.network)
    tx.addInput(sellerFunds)
    tx.addInput(buyerFunds)
    tx.addOutput((exchangeInfo.btcStepAmount * step).asSatoshi, buyersKey)
    tx.addOutput(
      (exchangeInfo.btcExchangeAmount - (exchangeInfo.btcStepAmount * step)).asSatoshi,
      sellersKey)
    tx
  }

  override def pay(step: Int): Future[Payment] = paymentProcessor.sendPayment(
    exchangeInfo.counterpartFiatAddress,
    exchangeInfo.fiatStepAmount,
    getPaymentDescription(step))

  override def getOffer(step: Int): Transaction =
    new Transaction(exchangeInfo.network, offers(step).bitcoinSerialize)

  override def validateSellersSignature(
      step: Int,
      signature0: TransactionSignature,
      signature1: TransactionSignature): Try[Unit] =
    validateSellersSignature(
      getOffer(step),
      signature0,
      signature1,
      s"The provided signature is invalid for the offer in step $step")

  override def finalOffer: Transaction = {
    val tx = new Transaction(exchangeInfo.network)
    tx.addInput(sellerFunds)
    tx.addInput(buyerFunds)
    tx.addOutput(
      (exchangeInfo.btcExchangeAmount + (exchangeInfo.btcStepAmount * 2)).asSatoshi,
      buyersKey)
    tx.addOutput(exchangeInfo.btcStepAmount.asSatoshi, sellersKey)
    tx
  }

  override def validatePayment(step: Int, paymentId: String): Future[Unit] = for {
    maybePayment <- paymentProcessor.findPayment(paymentId)
  } yield {
    require(maybePayment.isDefined, "PaymentId not found")
    val payment = maybePayment.get
    require(payment.amount == exchangeInfo.fiatStepAmount,
      "Payment amount does not match expected amount")
    require(payment.receiverId == sellersFiatAddress,
      "Payment is not being sent to the seller")
    require(payment.receiverId == buyersFiatAddress,
      "Payment is not coming from the buyer")
    require(payment.description == getPaymentDescription(step),
      "Payment does not have the required description")
    ()
  }

  override def validateSellersFinalSignature(
      signature0: TransactionSignature, signature1: TransactionSignature): Try[Unit] =
    validateSellersSignature(
      finalOffer,
      signature0,
      signature1,
      s"The provided signature is invalid for the final offer")

  override protected def sign(offer: Transaction): (TransactionSignature, TransactionSignature) = {
    val userConnectedPubKeyScript = ScriptBuilder.createMultiSigOutputScript(
      2, List(exchangeInfo.counterpartKey, exchangeInfo.userKey))
    val userInputSignature = offer.calculateSignature(
      userInputIndex,
      exchangeInfo.userKey,
      userConnectedPubKeyScript,
      SigHash.ALL,
      false)
    val counterpartConnectedPubKeyScript = ScriptBuilder.createMultiSigOutputScript(
      2, List(exchangeInfo.userKey, exchangeInfo.counterpartKey))
    val counterpartInputSignature = offer.calculateSignature(
      counterPartInputIndex,
      exchangeInfo.userKey,
      counterpartConnectedPubKeyScript,
      SigHash.ALL,
      false)
    if (userInputIndex == 0)
      (userInputSignature, counterpartInputSignature)
    else
      (counterpartInputSignature, userInputSignature)
  }

  private def getPaymentDescription(step: Int) = s"Payment for ${exchangeInfo.id}, step $step"

  private def validateSellersSignature(
      tx: Transaction,
      signature0: TransactionSignature,
      signature1: TransactionSignature,
      validationErrorMessage: String): Try[Unit] = Try {
    validateSellersSignature(tx, 0, signature0, validationErrorMessage)
    validateSellersSignature(tx, 1, signature1, validationErrorMessage)
  }

  private def validateSellersSignature(
      tx: Transaction,
      inputIndex: Int,
      signature: TransactionSignature,
      validationErrorMessage: String): Unit = {
    val input = tx.getInput(inputIndex)
    require(sellersKey.verify(
      tx.hashForSignature(
        0,
        input.getConnectedOutput.getScriptPubKey,
        SigHash.ALL,
        false),
      signature),
      s"Invalid signature for input $inputIndex: $validationErrorMessage")
  }
}
