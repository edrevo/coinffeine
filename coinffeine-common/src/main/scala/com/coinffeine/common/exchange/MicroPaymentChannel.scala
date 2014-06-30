package com.coinffeine.common.exchange

import scala.util.Try

import com.coinffeine.common.bitcoin.{ImmutableTransaction, TransactionSignature}
import com.coinffeine.common.exchange.Exchange.StepBreakdown
import com.coinffeine.common.exchange.MicroPaymentChannel._

trait MicroPaymentChannel {

  val currentStep: Step

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

  sealed trait Step {
    /** Step number in the range 1 to totalSteps */
    val value: Int

    val isFinal: Boolean

    /** Step after this one */
    @throws[IllegalArgumentException]("if this step is final")
    def next: Step
  }

  case class IntermediateStep(override val value: Int, breakdown: StepBreakdown) extends Step {
    require(value > 0, s"Step number must be positive ($value given)")
    require(value < breakdown.totalSteps,
      s"Step number must be less than ${breakdown.totalSteps} ($value given)")

    override val isFinal = false
    override def next =
      if (value == breakdown.intermediateSteps) FinalStep(breakdown) else copy(value = value + 1)
    override val toString = s"step $value/${breakdown.totalSteps}"
  }

  case class FinalStep(breakdown: StepBreakdown) extends Step {
    override val value = breakdown.totalSteps
    override val isFinal = true
    override def next = throw new IllegalArgumentException("Already at the last step")
    override def toString = s"step $value/$value"
  }

  /** Signatures for a step transaction of both deposits. */
  type Signatures = Both[TransactionSignature]
  val Signatures = Both

  case class InvalidSignaturesException(signatures: Signatures, cause: Throwable = null)
    extends IllegalArgumentException(s"Invalid signatures $signatures", cause)
}
