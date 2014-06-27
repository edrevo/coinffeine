package com.coinffeine.client.exchange

import scala.util.Try

import com.coinffeine.common.bitcoin.ImmutableTransaction
import com.coinffeine.common.exchange.MicroPaymentChannel._

trait ProtoMicroPaymentChannel {
  /** Validates that the signatures are valid for the offer in the passed in step */
  def validateStepTransactionSignatures(step: Step, signatures: Signatures): Try[Unit]

  /** Returns the transaction signature for the corresponding step */
  def signStepTransaction(step: Step): Signatures

  /** Returns a signed transaction ready to be broadcast */
  def closingTransaction(step: Step, herSignatures: Signatures): ImmutableTransaction
}
