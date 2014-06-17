package com.coinffeine.common.exchange.impl

import com.google.bitcoin.core.Transaction

import com.coinffeine.common.FiatCurrency
import com.coinffeine.common.exchange.{Closing, Deposits}

case class DefaultClosing[C <: FiatCurrency](
    override val exchange: DefaultExchange[C],
    lastTransaction: Transaction,
    deposits: Deposits) extends Closing[C](exchange) {

  private val buyerFundsAfter = {
    val lastStepFunds = exchange.amounts.buyerFundsAfter(exchange.amounts.totalSteps.lastStep)
    (lastStepFunds._1 + exchange.amounts.buyerDeposit, lastStepFunds._2)
  }

  private val sellerFundsAfter = {
    val lastStepFunds = exchange.amounts.sellerFundsAfter(exchange.amounts.totalSteps.lastStep)
    (exchange.amounts.sellerDeposit, lastStepFunds._2)
  }

  /** Transaction to settle the exchange.
    *
    * Note that this transaction lacks the required signatures and that a fresh instance is
    * returned every time as the Transaction class is mutable.
    */
  def closingTransaction: exchange.Transaction =
    TransactionProcessor.createTransaction(
      inputs = deposits.toSeq,
      outputs = Seq(
        exchange.buyer.bitcoinKey -> buyerFundsAfter._1,
        exchange.seller.bitcoinKey -> sellerFundsAfter._1))

  override def validateTransactionSignatures(
      buyerSignature: exchange.TransactionSignature,
      sellerSignature: exchange.TransactionSignature): Boolean =
    TransactionProcessor.areValidSignatures(closingTransaction, Seq(buyerSignature, sellerSignature))

  override def signTransaction: exchange.TransactionSignature = ???
}
