package com.coinffeine.common.exchange

import com.coinffeine.common.FiatCurrency

abstract class Handshake[C <: FiatCurrency](val exchange: Exchange[C]) {

  case class InvalidRefundSignature(
      refundTx: exchange.Transaction,
      invalidSignature: exchange.TransactionSignature) extends IllegalArgumentException(
    s"invalid signature $invalidSignature for refund transaction $refundTx")

  case class InvalidRefundTransaction(invalidTransaction: exchange.Transaction, cause: String)
    extends IllegalArgumentException(s"invalid refund transaction: $invalidTransaction: $cause")

  /** Ready to be broadcast deposit */
  def myDeposit: exchange.Transaction

  def myUnsignedRefund: exchange.Transaction

  @throws[InvalidRefundSignature]
  def signMyRefund(herSignature: exchange.TransactionSignature): exchange.Transaction

  @throws[InvalidRefundTransaction]
  def signHerRefund(herRefund: exchange.Transaction): exchange.TransactionSignature

  def createMicroPaymentChannel(herDeposit: exchange.Transaction): MicroPaymentChannel[C]
}
