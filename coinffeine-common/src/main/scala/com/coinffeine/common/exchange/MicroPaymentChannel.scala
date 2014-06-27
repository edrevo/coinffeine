package com.coinffeine.common.exchange

import com.coinffeine.common.bitcoin.{ImmutableTransaction, TransactionSignature}
import com.coinffeine.common.exchange.MicroPaymentChannel._

trait MicroPaymentChannel {

  def currentStep: Step

  def validateCurrentTransactionSignatures(herSignatures: StepSignatures): Boolean

  def signCurrentTransaction: StepSignatures

  def nextStep: MicroPaymentChannel

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

  sealed trait Step
  case class IntermediateStep(value: Int) extends Step {
    require(value > 0, s"Step number must be positive ($value given)")
  }
  case object FinalStep extends Step

  /** Signatures for a step transaction of both deposits. */
  case class StepSignatures(buyerDepositSignature: TransactionSignature,
                            sellerDepositSignature: TransactionSignature) {
    def toTuple: (TransactionSignature, TransactionSignature) =
      (buyerDepositSignature, sellerDepositSignature)
  }
}
