package com.coinffeine.client.exchange

import scala.concurrent.Future
import scala.util.Try

import com.google.bitcoin.core.Transaction
import com.google.bitcoin.crypto.TransactionSignature

import com.coinffeine.client.ExchangeInfo
import com.coinffeine.common.FiatCurrency
import com.coinffeine.common.paymentprocessor.AnyPayment

trait Exchange[C <: FiatCurrency] {
  this: UserRole =>

  val exchangeInfo: ExchangeInfo[C]

  /** Returns the bitcoin transaction that corresponds with the offer for the passed in step */
  def getOffer(step: Int): Transaction

  /** Returns the transaction signature for the corresponding step */
  def signStep(step: Int): (TransactionSignature, TransactionSignature) = sign(getOffer(step))

  /** Returns the bitcoin transaction that corresponds with the final offer */
  def finalOffer: Transaction

  /** Returns the transaction signature for the final step */
  def finalSignature: (TransactionSignature, TransactionSignature) = sign(finalOffer)

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
  protected def sign(offer: Transaction): (TransactionSignature, TransactionSignature)
}
