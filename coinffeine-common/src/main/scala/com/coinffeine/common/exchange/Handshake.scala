package com.coinffeine.common.exchange

import com.coinffeine.common.bitcoin.{ImmutableTransaction, TransactionSignature}
import com.coinffeine.common.exchange.Handshake.{InvalidRefundSignature, InvalidRefundTransaction}

trait Handshake {

  /** Ready to be broadcast deposit */
  def myDeposit: ImmutableTransaction

  def myUnsignedRefund: ImmutableTransaction

  @throws[InvalidRefundSignature]
  def signMyRefund(herSignature: TransactionSignature): ImmutableTransaction

  @throws[InvalidRefundTransaction]("refund transaction was not valid (e.g. incorrect amount)")
  def signHerRefund(herRefund: ImmutableTransaction): TransactionSignature
}

object Handshake {

  case class InvalidRefundSignature(
      refundTx: ImmutableTransaction,
      invalidSignature: TransactionSignature) extends IllegalArgumentException(
    s"invalid signature $invalidSignature for refund transaction $refundTx")

  case class InvalidRefundTransaction(invalidTransaction: ImmutableTransaction, cause: String)
    extends IllegalArgumentException(s"invalid refund transaction: $invalidTransaction: $cause")
}
