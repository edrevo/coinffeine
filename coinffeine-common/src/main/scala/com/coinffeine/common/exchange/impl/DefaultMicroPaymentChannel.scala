package com.coinffeine.common.exchange.impl

import com.google.bitcoin.core.Transaction

import com.coinffeine.common.FiatCurrency
import com.coinffeine.common.exchange.{Deposits, Exchange, MicroPaymentChannel}

case class DefaultMicroPaymentChannel[C <: FiatCurrency](
    override val exchange: DefaultExchange[C],
    override val deposits: Deposits,
    override val currentStep: Exchange.StepNumber = Exchange.StepNumber.First)
  extends MicroPaymentChannel[C](exchange) {

  override val currentTransaction: Transaction =
    TransactionProcessor.createUnsignedTransaction(
      inputs = deposits.toSeq,
      outputs = Seq(
        exchange.buyer.bitcoinKey -> buyerFundsAfterCurrentStep._1,
        exchange.seller.bitcoinKey -> sellerFundsAfterCurrentStep._1),
      network = exchange.parameters.network
    )

  override def validateCurrentTransactionSignatures(
      buyerSignature: exchange.TransactionSignature,
      sellerSignature: exchange.TransactionSignature): Boolean =
    TransactionProcessor.areValidSignatures(currentTransaction, Seq(buyerSignature, sellerSignature))

  override def signCurrentTransaction: exchange.TransactionSignature = ???

  override def nextStep: DefaultMicroPaymentChannel[C] = copy(currentStep = currentStep.next)

  override def close(): DefaultClosing[C] = DefaultClosing(exchange, currentTransaction, deposits)
}
