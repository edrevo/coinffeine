package com.coinffeine.common.exchange

import com.coinffeine.common.FiatCurrency

abstract class Handshake[C <: FiatCurrency](val exchange: Exchange[C]) {

  case class InvalidRefundSignature(
      refundTx: exchange.Transaction,
      invalidSignature: exchange.TransactionSignature) extends IllegalArgumentException(
    s"invalid signature $invalidSignature for refund transaction $refundTx")

  def myDeposit: exchange.Transaction
  def myRefund: exchange.Transaction

  def myRefundIsSigned: Boolean

  @throws[InvalidRefundSignature]
  def withHerSignatureOfMyRefund(signature: exchange.TransactionSignature): Handshake[C]

  def startExchange(herDeposit: exchange.Transaction): MicroPaymentChannel[C]
}
