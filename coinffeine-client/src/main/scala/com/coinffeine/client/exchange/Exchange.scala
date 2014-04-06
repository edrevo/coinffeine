package com.coinffeine.client.exchange

import scala.concurrent.Future

import com.google.bitcoin.core.{ECKey, Transaction}
import com.google.bitcoin.crypto.TransactionSignature

import com.coinffeine.common.paymentprocessor.Payment

trait Exchange {
  /** Returns the bitcoin transaction that corresponds with the offer for the passed in step */
  def getOffer(step: Int): Transaction

  /** Returns the bitcoin transaction that corresponds with the final offer */
  val finalOffer: Transaction

  /** Validates that the signature is valid for the offer in the passed in step */
  def validateSignature(step: Int, signature: TransactionSignature): Boolean

  /** Validates that the signature is valid for the final offer */
  def validateFinalSignature(signature: TransactionSignature): Boolean

  def sign(offer: Transaction, key: ECKey): TransactionSignature

  def validatePayment(step: Int, paymentId: String): Boolean

  /** Performs a payment for a step of the exchange */
  def pay(step: Int): Future[Payment]
}
