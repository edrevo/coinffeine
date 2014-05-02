package com.coinffeine.client.exchange

import scala.concurrent.Future
import scala.util.Try

import com.google.bitcoin.core.Transaction
import com.google.bitcoin.crypto.TransactionSignature

import com.coinffeine.client.ExchangeInfo
import com.coinffeine.common.paymentprocessor.Payment

trait Exchange {
  val exchangeInfo: ExchangeInfo

  /** Returns the bitcoin transaction that corresponds with the offer for the passed in step */
  def getOffer(step: Int): Transaction

  /** Returns the transaction signature for the corresponding step */
  def signStep(step: Int): TransactionSignature = sign(getOffer(step))

  /** Returns the bitcoin transaction that corresponds with the final offer */
  def finalOffer: Transaction

  /** Returns the transaction signature for the final step */
  def finalSignature: TransactionSignature = sign(finalOffer)

  /** Validates that the signature is valid for the offer in the passed in step */
  def validateSignature(step: Int, signature: TransactionSignature): Try[Unit]

  /** Validates that the signature is valid for the final offer */
  def validateFinalSignature(signature: TransactionSignature): Try[Unit]

  def validatePayment(step: Int, paymentId: String): Try[Unit]

  /** Performs a payment for a step of the exchange */
  def pay(step: Int): Future[Payment]

  protected def sign(offer: Transaction): TransactionSignature
}
