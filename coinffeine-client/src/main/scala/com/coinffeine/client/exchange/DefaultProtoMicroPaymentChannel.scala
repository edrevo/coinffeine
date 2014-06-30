package com.coinffeine.client.exchange

import scala.util.Try
import scala.util.control.NonFatal

import com.coinffeine.common.{BitcoinAmount, FiatCurrency}
import com.coinffeine.common.bitcoin._
import com.coinffeine.common.exchange._
import com.coinffeine.common.exchange.MicroPaymentChannel._
import com.coinffeine.common.exchange.impl.TransactionProcessor

class DefaultProtoMicroPaymentChannel(
    exchange: Exchange[_ <: FiatCurrency],
    role: Role,
    deposits: Exchange.Deposits) extends ProtoMicroPaymentChannel {

  import com.coinffeine.client.exchange.DefaultProtoMicroPaymentChannel._

  private val requiredSignatures = Seq(exchange.buyer.bitcoinKey, exchange.seller.bitcoinKey)
  private val buyerFunds = deposits.transactions.buyer.get.getOutput(0)
  private val sellerFunds = deposits.transactions.seller.get.getOutput(0)

  private def getOffer(step: Step): MutableTransaction =
    if (step.isFinal) getOffer(
      buyerAmount = exchange.parameters.bitcoinAmount + exchange.amounts.stepBitcoinAmount * 2,
      sellerAmount = exchange.amounts.stepBitcoinAmount
    ) else getOffer(
      buyerAmount = exchange.amounts.stepBitcoinAmount * step.value,
      sellerAmount = exchange.parameters.bitcoinAmount - exchange.amounts.stepBitcoinAmount * step.value
    )

  private def getOffer(buyerAmount: BitcoinAmount, sellerAmount: BitcoinAmount): MutableTransaction =
    TransactionProcessor.createUnsignedTransaction(
      inputs = Seq(buyerFunds, sellerFunds),
      outputs = Seq(
        exchange.buyer.bitcoinKey -> buyerAmount,
        exchange.seller.bitcoinKey -> sellerAmount),
      network = exchange.parameters.network
    )

  override def validateStepTransactionSignatures(step: Step, signatures: Signatures): Try[Unit] =
    validateSellersSignature(
      getOffer(step),
      signatures,
      s"The provided signature is invalid for the offer in $step"
    )

  override def signStepTransaction(step: Step): Signatures = sign(getOffer(step))

  private def sign(offer: MutableTransaction) = {
    val key = role.me(exchange).bitcoinKey
    Signatures(
      buyer = TransactionProcessor.signMultiSignedOutput(offer, 0, key, requiredSignatures),
      seller = TransactionProcessor.signMultiSignedOutput(offer, 1, key, requiredSignatures)
    )
  }

  private def validateSellersSignature(
      tx: MutableTransaction,
      herSignatures: Signatures,
      validationErrorMessage: String): Try[Unit] = Try {
    validateSellersSignature(
      tx, BuyerDepositInputIndex, herSignatures.buyer, validationErrorMessage)
    validateSellersSignature(
      tx, SellerDepositInputIndex, herSignatures.seller, validationErrorMessage)
  } recover {
    case NonFatal(cause) => throw InvalidSignaturesException(herSignatures, cause)
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
  override def closingTransaction(step: Step, herSignatures: Signatures) = {
    val tx = getOffer(step)
    val signatures = Seq(sign(tx), herSignatures)
    TransactionProcessor.setMultipleSignatures(tx, BuyerDepositInputIndex, signatures.map(_.buyer): _*)
    TransactionProcessor.setMultipleSignatures(tx, SellerDepositInputIndex, signatures.map(_.seller): _*)
    ImmutableTransaction(tx)
  }
}

object DefaultProtoMicroPaymentChannel {
  val BuyerDepositInputIndex = 0
  val SellerDepositInputIndex = 1
}
