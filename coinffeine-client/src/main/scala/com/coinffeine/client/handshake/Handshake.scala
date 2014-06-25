package com.coinffeine.client.handshake

import com.coinffeine.common.FiatCurrency
import com.coinffeine.common.bitcoin.{ImmutableTransaction, TransactionSignature}
import com.coinffeine.common.exchange.{Exchange, Role}
import com.coinffeine.common.exchange.Handshake.{InvalidRefundSignature, InvalidRefundTransaction}

trait Handshake[C <: FiatCurrency] {

  def myDeposit: ImmutableTransaction
  def myUnsignedRefund: ImmutableTransaction

  @throws[InvalidRefundSignature]
  def signMyRefund(herSignature: TransactionSignature): ImmutableTransaction

  @throws[InvalidRefundTransaction]("refund transaction was not valid (e.g. incorrect amount)")
  def signHerRefund(herRefund: ImmutableTransaction): TransactionSignature
}
