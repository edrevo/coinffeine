package com.coinffeine.common.exchange

import scala.util.Try

import com.coinffeine.common.bitcoin.{ImmutableTransaction, TransactionSignature}
import com.coinffeine.common.exchange.MicroPaymentChannel._

trait MicroPaymentChannel {

  def currentStep: Step

  def nextStep: MicroPaymentChannel

  /** Check signature validity for the current step.
    *
    * @param herSignatures  Counterpart signatures for buyer and seller deposits
    * @return               A success is everything is correct or a failure with an
    *                       [[InvalidSignaturesException]] otherwise
    */
  def validateCurrentTransactionSignatures(herSignatures: Signatures): Try[Unit]

  def signCurrentTransaction: Signatures

  /** Given valid counterpart signatures it generates the closing transaction.
    *
    * The resulting transaction contains the following funds:
    *
    *  * For the last transaction in the happy path scenario it contains both the exchanged
    *    amount for the buyer and the deposits for each participant.
    *  * For an intermediate step, just the confirmed steps amounts for the buyer and the
    *    rest of the amount to exchange for the seller. Note that deposits are lost as fees.
    *
    * @param herSignatures  Valid counterpart signatures
    * @return               Ready to broadcast transaction
    */
  @throws[InvalidSignaturesException]("if herSignatures are not valid")
  def closingTransaction(herSignatures: Signatures): ImmutableTransaction
}

object MicroPaymentChannel {

  sealed trait Step
  case class IntermediateStep(value: Int) extends Step {
    require(value > 0, s"Step number must be positive ($value given)")
  }
  case object FinalStep extends Step

  /** Signatures for a step transaction of both deposits. */
  case class Signatures(buyerDepositSignature: TransactionSignature,
                        sellerDepositSignature: TransactionSignature) {
    def toTuple: (TransactionSignature, TransactionSignature) =
      (buyerDepositSignature, sellerDepositSignature)
  }

  case class InvalidSignaturesException(signatures: Signatures, cause: Throwable = null)
    extends IllegalArgumentException(s"Invalid signatures $signatures", cause)
}
