package com.coinffeine.common.protocol.messages.exchange

import com.coinffeine.common.bitcoin.TransactionSignature
import com.coinffeine.common.protocol.TransactionSignatureUtils
import com.coinffeine.common.protocol.messages.PublicMessage

/** This message contains the seller's signatures for a step in a specific exchange
  * @param exchangeId The exchange id for which the signatures are valid
  * @param step The step number for which the signatures are valid
  * @param idx0Signature The signature for input 0 in the step
  * @param idx1Signature The signature for input 1 in the step
  */
case class StepSignatures(
    exchangeId: ExchangeId,
    step: Int,
    idx0Signature: TransactionSignature,
    idx1Signature: TransactionSignature)  extends PublicMessage {

  override def equals(that: Any) = that match {
    case newStepStart: StepSignatures => (newStepStart.exchangeId == exchangeId) &&
      TransactionSignatureUtils.equals(newStepStart.idx0Signature, idx0Signature) &&
      TransactionSignatureUtils.equals(newStepStart.idx1Signature, idx1Signature)
    case _ => false
  }
}

object StepSignatures {

  /** Factory method accepting a tuple of signatures. */
  def apply(exchangeId: ExchangeId,
            step: Int,
            signatures: (TransactionSignature, TransactionSignature)): StepSignatures =
    StepSignatures(exchangeId, step, signatures._1, signatures._2)
}
