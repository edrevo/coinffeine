package com.coinffeine.common.exchange

import com.coinffeine.common.FiatCurrency
import com.coinffeine.common.bitcoin.{ImmutableTransaction, TransactionSignature}

trait Handshake[C <: FiatCurrency] {

  case class InvalidRefundSignature(
      refundTx: ImmutableTransaction,
      invalidSignature: TransactionSignature) extends IllegalArgumentException(
    s"invalid signature $invalidSignature for refund transaction $refundTx")

  case class InvalidRefundTransaction(invalidTransaction: ImmutableTransaction, cause: String)
    extends IllegalArgumentException(s"invalid refund transaction: $invalidTransaction: $cause")

  /** Ready to be broadcast deposit */
  def myDeposit: ImmutableTransaction

  def myUnsignedRefund: ImmutableTransaction

  @throws[InvalidRefundSignature]
  def signMyRefund(herSignature: TransactionSignature): ImmutableTransaction

  @throws[InvalidRefundTransaction]
  def signHerRefund(herRefund: ImmutableTransaction): TransactionSignature

  def createMicroPaymentChannel(herDeposit: ImmutableTransaction): MicroPaymentChannel[C]
}
