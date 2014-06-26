package com.coinffeine.client.exchange

import scala.util.Try

import akka.actor.ActorRef

import com.coinffeine.client.MultiSigInfo
import com.coinffeine.common.{BitcoinAmount, Currency, FiatCurrency}
import com.coinffeine.common.bitcoin._
import com.coinffeine.common.exchange.{Exchange, Role}
import com.coinffeine.common.exchange.MicroPaymentChannel.StepSignatures
import com.coinffeine.common.exchange.impl.TransactionProcessor
import com.coinffeine.common.paymentprocessor.PaymentProcessor

class DefaultProtoMicroPaymentChannel[C <: FiatCurrency](
    exchange: Exchange[C],
    role: Role,
    paymentProcessor: ActorRef,
    sellerCommitmentTx: ImmutableTransaction,
    buyerCommitmentTx: ImmutableTransaction) extends ProtoMicroPaymentChannel[C] {

  import com.coinffeine.client.exchange.DefaultProtoMicroPaymentChannel._

  private implicit val paymentProcessorTimeout = PaymentProcessor.RequestTimeout
  private val requiredSignatures = Seq(exchange.buyer.bitcoinKey, exchange.seller.bitcoinKey)
  private val sellerFunds = sellerCommitmentTx.get.getOutput(0)
  private val buyerFunds = buyerCommitmentTx.get.getOutput(0)
  requireValidSellerFunds(sellerFunds)
  requireValidBuyerFunds(buyerFunds)

  private def requireValidFunds(funds: MutableTransactionOutput): Unit = {
    require(funds.getScriptPubKey.isSentToMultiSig,
      "Transaction with funds is invalid because is not sending the funds to a multisig")
    val multisigInfo = MultiSigInfo(funds.getScriptPubKey)
    require(multisigInfo.requiredKeyCount == 2,
      "Funds are sent to a multisig that do not require 2 keys")
    require(multisigInfo.possibleKeys == requiredSignatures.toSet,
      "Possible keys in multisig script does not match the expected keys")
  }

  private def requireValidBuyerFunds(buyerFunds: MutableTransactionOutput): Unit = {
    requireValidFunds(buyerFunds)
    require(Currency.Bitcoin.fromSatoshi(buyerFunds.getValue) == exchange.amounts.stepBitcoinAmount * 2,
      "The amount of committed funds by the buyer does not match the expected amount")
  }

  private def requireValidSellerFunds(sellerFunds: MutableTransactionOutput): Unit = {
    requireValidFunds(sellerFunds)
    require(
      Currency.Bitcoin.fromSatoshi(sellerFunds.getValue) ==
        exchange.parameters.bitcoinAmount + exchange.amounts.stepBitcoinAmount,
      "The amount of committed funds by the seller does not match the expected amount")
  }

  override def getOffer(step: Int): MutableTransaction = getOffer(
    buyerAmount = exchange.amounts.stepBitcoinAmount * step,
    sellerAmount = exchange.parameters.bitcoinAmount - exchange.amounts.stepBitcoinAmount * step
  )

  override def finalOffer: MutableTransaction = getOffer(
    buyerAmount = exchange.parameters.bitcoinAmount + exchange.amounts.stepBitcoinAmount * 2,
    sellerAmount = exchange.amounts.stepBitcoinAmount
  )

  private def getOffer(buyerAmount: BitcoinAmount, sellerAmount: BitcoinAmount): MutableTransaction =
    TransactionProcessor.createUnsignedTransaction(
      inputs = Seq(buyerFunds, sellerFunds),
      outputs = Seq(
        exchange.buyer.bitcoinKey -> buyerAmount,
        exchange.seller.bitcoinKey -> sellerAmount),
      network = exchange.parameters.network
    )

  override def validateSellersSignature(
      step: Int,
      signature0: TransactionSignature,
      signature1: TransactionSignature): Try[Unit] =
    validateSellersSignature(
      getOffer(step),
      signature0,
      signature1,
      s"The provided signature is invalid for the offer in step $step")

  override def validateSellersFinalSignature(
      signature0: TransactionSignature, signature1: TransactionSignature): Try[Unit] =
    validateSellersSignature(
      finalOffer,
      signature0,
      signature1,
      s"The provided signature is invalid for the final offer")

  override protected def sign(offer: MutableTransaction) = {
    val key = role.me(exchange).bitcoinKey
    StepSignatures(
      buyerDepositSignature =
        TransactionProcessor.signMultiSignedOutput(offer, 0, key, requiredSignatures),
      sellerDepositSignature =
        TransactionProcessor.signMultiSignedOutput(offer, 1, key, requiredSignatures)
    )
  }

  private def getPaymentDescription(step: Int) = PaymentDescription(exchange.id, step)

  private def validateSellersSignature(
      tx: MutableTransaction,
      signature0: TransactionSignature,
      signature1: TransactionSignature,
      validationErrorMessage: String): Try[Unit] = Try {
    validateSellersSignature(tx, 0, signature0, validationErrorMessage)
    validateSellersSignature(tx, 1, signature1, validationErrorMessage)
  }

  private def validateSellersSignature(
      tx: MutableTransaction,
      inputIndex: Int,
      signature: TransactionSignature,
      validationErrorMessage: String): Unit = {
    require(
      TransactionProcessor.isValidSignature(tx, inputIndex, signature, exchange.seller.bitcoinKey, requiredSignatures),
      s"Invalid signature for input $inputIndex: $validationErrorMessage"
    )
  }

  /** Returns a signed transaction ready to be broadcast */
  override def getSignedOffer(step: Int, herSignatures: StepSignatures) = {
    val tx = getOffer(step)
    val signatures = Seq(sign(tx), herSignatures)
    TransactionProcessor.setMultipleSignatures(tx, BuyerDepositInputIndex, signatures.map(_.buyerDepositSignature): _*)
    TransactionProcessor.setMultipleSignatures(tx, SellerDepositInputIndex, signatures.map(_.sellerDepositSignature): _*)
    tx
  }
}

object DefaultProtoMicroPaymentChannel {
  val BuyerDepositInputIndex = 0
  val SellerDepositInputIndex = 1
}
