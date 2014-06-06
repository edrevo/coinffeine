package com.coinffeine.client.handshake

import scala.util.Try

import com.google.bitcoin.core.Transaction
import com.google.bitcoin.crypto.TransactionSignature

import com.coinffeine.client.ExchangeInfo
import com.coinffeine.common.FiatCurrency

trait Handshake[C <: FiatCurrency] {
  val exchangeInfo: ExchangeInfo[C]
  val commitmentTransaction: Transaction
  val refundTransaction: Transaction

  /** Signs counterpart refundSignature transaction
    *
    * @param refundTransaction  Transaction to sign
    * @return  TX signature or a failure if the refundSignature was not valid (e.g. incorrect
    *          amount)
    */
  def signCounterpartRefundTransaction(refundTransaction: Transaction): Try[TransactionSignature]

  /** Validate counterpart signature of our refundSignature transaction.
    *
    * @param signature  Signed refundSignature TX
    * @return           Success when valid or rejection cause as an exception
    */
  def validateRefundSignature(signature: TransactionSignature): Try[Unit]
}
