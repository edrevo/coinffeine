package com.coinffeine.common.protocol.messages.exchange

import com.google.bitcoin.crypto.TransactionSignature

import com.coinffeine.common.protocol.messages.PublicMessage
import com.coinffeine.common.protocol.TransactionSignatureUtils

case class StepSignature(exchangeId: String, signature: TransactionSignature)
  extends PublicMessage {

  override def equals(that: Any) = that match {
    case newStepStart: StepSignature => (newStepStart.exchangeId == exchangeId) &&
      TransactionSignatureUtils.equals(newStepStart.signature, signature)
    case _ => false
  }
}
