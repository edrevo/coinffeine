package com.coinffeine.client.handshake

import scala.util.Try

import com.coinffeine.common.FiatCurrency
import com.coinffeine.common.bitcoin.{ImmutableTransaction, TransactionSignature}
import com.coinffeine.common.exchange.{Exchange, Role}
import com.coinffeine.common.exchange.Handshake.InvalidRefundTransaction

trait Handshake[C <: FiatCurrency] {
  val exchange: Exchange[C]
  val role: Role

  def myDeposit: ImmutableTransaction
  def myUnsignedRefund: ImmutableTransaction

  @throws[InvalidRefundTransaction]("refund transaction was not valid (e.g. incorrect amount)")
  def signHerRefund(refundTransaction: ImmutableTransaction): TransactionSignature

  /** Validate counterpart signature of our refundSignature transaction.
    *
    * @param signature  Signed refundSignature TX
    * @return           Success when valid or rejection cause as an exception
    */
  def validateRefundSignature(signature: TransactionSignature): Try[Unit]
}
