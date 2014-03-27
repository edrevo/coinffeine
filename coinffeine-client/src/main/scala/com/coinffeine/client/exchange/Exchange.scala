package com.coinffeine.client.exchange

import scala.concurrent.Future

import com.google.bitcoin.core.Transaction
import com.google.bitcoin.crypto.TransactionSignature

import com.coinffeine.common.paymentprocessor.Payment

trait Exchange {
  /** Returns the bitcoin transaction that corresponds with the offer for the passed in step */
  def getOffer(step: Int): Transaction

  /** Validates that the signature is valid for the offer in the passed in step */
  def validateSignature(step: Int, signature: TransactionSignature): Boolean

  /** Performs a payment for a step of the exchange */
  def pay(step: Int): Future[Payment]

  /** Returns true if the buyer must pay in this step, false otherwise (which indicates the
    * exchange is done) */
  def mustPay(step: Int): Boolean
}
