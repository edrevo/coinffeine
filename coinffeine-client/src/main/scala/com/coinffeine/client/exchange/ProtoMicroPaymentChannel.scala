package com.coinffeine.client.exchange

import scala.util.Try

import com.coinffeine.common.FiatCurrency
import com.coinffeine.common.bitcoin.{MutableTransaction, TransactionSignature}
import com.coinffeine.common.exchange.MicroPaymentChannel._

trait ProtoMicroPaymentChannel[C <: FiatCurrency] {
  /** Returns the bitcoin transaction that corresponds with the offer for the passed in step */
  def getOffer(step: Step): MutableTransaction

  /** Returns a signed transaction ready to be broadcast */
  def getSignedOffer(step: IntermediateStep, counterpartSignatures: StepSignatures): MutableTransaction

  /** Returns the transaction signature for the corresponding step */
  def signStep(step: Step): StepSignatures = sign(getOffer(step))

  /** Returns the transaction signature for the final step */
  def signFinalStep: StepSignatures = signStep(FinalStep)

  /** Validates that the signatures are valid for the offer in the passed in step */
  def validateSellersSignature(
    step: Step, signature0: TransactionSignature, signature1: TransactionSignature): Try[Unit]

  /** Returns the user's signature for the transaction */
  protected def sign(offer: MutableTransaction): StepSignatures
}
