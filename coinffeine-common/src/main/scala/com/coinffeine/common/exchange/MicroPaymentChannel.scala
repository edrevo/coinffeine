package com.coinffeine.common.exchange

import com.coinffeine.common.FiatCurrency
import com.coinffeine.common.bitcoin.{ImmutableTransaction, TransactionSignature}
import com.coinffeine.common.exchange.MicroPaymentChannel.StepSignatures

trait MicroPaymentChannel[C <: FiatCurrency] {

  def deposits: Deposits[ImmutableTransaction]

  def currentStep: Exchange.StepNumber

  def validateCurrentTransactionSignatures(herSignatures: StepSignatures): Boolean

  def signCurrentTransaction: StepSignatures

  def nextStep: MicroPaymentChannel[C]

  /** Given valid counterpart signatures it generates the closing transaction.
    *
    * The resulting transaction contains the following funds:
    *
    *  * For the last transaction in the happy path scenario it contains both the exchanged
    *    amount for the buyer and the deposits for each participant.
    *  * For an intermediate step, just the confirmed steps amounts for the buyer and the
    *    rest of the amount to exchange for the seller. Note that deposits are lost as fees.
    */
  def closingTransaction(herSignatures: StepSignatures): ImmutableTransaction
}

object MicroPaymentChannel {
  /** Signatures for a step transaction of both deposits. */
  case class StepSignatures(buyerDepositSignature: TransactionSignature,
                            sellerDepositSignature: TransactionSignature)
}
