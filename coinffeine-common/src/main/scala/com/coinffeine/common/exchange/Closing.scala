package com.coinffeine.common.exchange

import com.coinffeine.common.FiatCurrency

abstract class Closing[C <: FiatCurrency](val exchange: Exchange[C]) {

  def validateTransactionSignatures(
      buyerSignature: exchange.TransactionSignature,
      sellerSignature: exchange.TransactionSignature): Boolean

  def closingTransaction: exchange.Transaction
  def signTransaction: exchange.TransactionSignature
}
