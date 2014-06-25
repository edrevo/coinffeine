package com.coinffeine.client.handshake

import scala.util.Try

import com.coinffeine.client.ExchangeInfo
import com.coinffeine.common.FiatCurrency
import com.coinffeine.common.bitcoin.{ImmutableTransaction, MutableTransaction, TransactionSignature}

trait Handshake[C <: FiatCurrency] {
  val exchangeInfo: ExchangeInfo[C]
  val commitmentTransaction: ImmutableTransaction
  val unsignedRefundTransaction: ImmutableTransaction

  /** Signs counterpart refundSignature transaction
    *
    * @param refundTransaction  Transaction to sign
    * @return  TX signature or a failure if the refundSignature was not valid (e.g. incorrect
    *          amount)
    */
  def signCounterpartRefundTransaction(refundTransaction: MutableTransaction): Try[TransactionSignature]

  /** Validate counterpart signature of our refundSignature transaction.
    *
    * @param signature  Signed refundSignature TX
    * @return           Success when valid or rejection cause as an exception
    */
  def validateRefundSignature(signature: TransactionSignature): Try[Unit]
}
