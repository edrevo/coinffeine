package com.coinffeine.common.exchange

import com.coinffeine.common.bitcoin.ImmutableTransaction

case class Deposits(buyerDeposit: ImmutableTransaction, sellerDeposit: ImmutableTransaction) {
  def toSeq: Seq[ImmutableTransaction] = Seq(buyerDeposit, sellerDeposit)
}
