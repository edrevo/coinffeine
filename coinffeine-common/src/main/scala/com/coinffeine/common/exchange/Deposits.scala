package com.coinffeine.common.exchange

import com.google.bitcoin.core.TransactionOutput

case class Deposits(buyerOutput: TransactionOutput, sellerOutput: TransactionOutput) {
  def toSeq = Seq(buyerOutput, sellerOutput)
}
