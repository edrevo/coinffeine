package com.coinffeine.client.exchange

import scala.concurrent.Future
import scala.util.Try

import com.coinffeine.client.ExchangeInfo
import com.coinffeine.common.FiatCurrency
import com.coinffeine.common.bitcoin.{MutableTransaction, TransactionSignature}
import com.coinffeine.common.exchange.MicroPaymentChannel.StepSignatures
import com.coinffeine.common.paymentprocessor.AnyPayment

trait ProtoMicroPaymentChannel[C <: FiatCurrency] {
  this: UserRole =>

  val exchangeInfo: ExchangeInfo[C]

  /** Returns the bitcoin transaction that corresponds with the offer for the passed in step */
  def getOffer(step: Int): MutableTransaction

  /** Returns a signed transaction ready to be broadcast */
  def getSignedOffer(step: Int, counterpartSignatures: StepSignatures): MutableTransaction

  /** Returns the transaction signature for the corresponding step */
  def signStep(step: Int): StepSignatures = sign(getOffer(step))

  /** Returns the bitcoin transaction that corresponds with the final offer */
  def finalOffer: MutableTransaction

  /** Returns the transaction signature for the final step */
  def finalSignatures: StepSignatures = sign(finalOffer)

  /** Validates that the signatures are valid for the offer in the passed in step */
  def validateSellersSignature(
    step: Int, signature0: TransactionSignature, signature1: TransactionSignature): Try[Unit]

  /** Validates that the signatures are valid for the final offer */
  def validateSellersFinalSignature(
    signature0: TransactionSignature, signature1: TransactionSignature): Try[Unit]

  /** Validates that the paymentId represents a valid payment for the step */
  def validatePayment(step: Int, paymentId: String): Future[Unit]

  /** Performs a payment for a step of the exchange */
  def pay(step: Int): Future[AnyPayment]

  /** Returns the user's signature for the transaction */
  protected def sign(offer: MutableTransaction): StepSignatures
}
