package com.bitwise.bitmarket.client.handshake

import scala.util.Try

import com.google.bitcoin.core.Transaction
import com.google.bitcoin.crypto.TransactionSignature

import com.bitwise.bitmarket.client.Exchange

trait ExchangeHandshake {
  val exchange: Exchange
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
