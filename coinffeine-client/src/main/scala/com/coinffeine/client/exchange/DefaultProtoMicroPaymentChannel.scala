package com.coinffeine.client.exchange

import scala.util.Try
import scala.util.control.NonFatal

import com.coinffeine.client.MultiSigInfo
import com.coinffeine.common.{BitcoinAmount, Currency, FiatCurrency}
import com.coinffeine.common.bitcoin._
import com.coinffeine.common.exchange.{Deposits, Exchange, ProtoMicroPaymentChannel, Role}
import com.coinffeine.common.exchange.MicroPaymentChannel._
import com.coinffeine.common.exchange.impl.TransactionProcessor

class DefaultProtoMicroPaymentChannel(
    exchange: Exchange[_ <: FiatCurrency],
    role: Role,
    deposits: Deposits) extends ProtoMicroPaymentChannel {

  import com.coinffeine.client.exchange.DefaultProtoMicroPaymentChannel._

  private val requiredSignatures = Seq(exchange.buyer.bitcoinKey, exchange.seller.bitcoinKey)
  private val buyerFunds = deposits.buyer.get.getOutput(0)
  private val sellerFunds = deposits.seller.get.getOutput(0)
  requireValidBuyerFunds(buyerFunds)
  requireValidSellerFunds(sellerFunds)

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

  private def getOffer(step: Step): MutableTransaction = step match {
    case IntermediateStep(i) =>
      getOffer(
        buyerAmount = exchange.amounts.stepBitcoinAmount * i,
        sellerAmount = exchange.parameters.bitcoinAmount - exchange.amounts.stepBitcoinAmount * i
      )
    case FinalStep =>
      getOffer(
        buyerAmount = exchange.parameters.bitcoinAmount + exchange.amounts.stepBitcoinAmount * 2,
        sellerAmount = exchange.amounts.stepBitcoinAmount
      )
  }

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
