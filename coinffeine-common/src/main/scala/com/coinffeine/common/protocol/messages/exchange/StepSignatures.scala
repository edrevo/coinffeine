package com.coinffeine.common.protocol.messages.exchange

import com.coinffeine.common.exchange.Exchange
import com.coinffeine.common.exchange.MicroPaymentChannel.Signatures
import com.coinffeine.common.protocol.TransactionSignatureUtils
import com.coinffeine.common.protocol.messages.PublicMessage

/** This message contains the seller's signatures for a step in a specific exchange
  * @param exchangeId The exchange id for which the signatures are valid
  * @param step The step number for which the signatures are valid
  * @param signatures The signatures for buyer and seller inputs for the step
  */
case class StepSignatures(exchangeId: Exchange.Id, step: Int, signatures: Signatures)
  extends PublicMessage {

  override def equals(other: Any) = other match {
    case that: StepSignatures =>
      (that.exchangeId == exchangeId) && (that.step == step) &&
      TransactionSignatureUtils.equals(
        that.signatures.buyer, signatures.buyer) &&
      TransactionSignatureUtils.equals(
        that.signatures.seller, signatures.seller)
    case _ => false
  }

  // TODO: missing hashCode
}
