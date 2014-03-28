package com.coinffeine.common.protocol.messages.handshake

import com.google.bitcoin.crypto.TransactionSignature

import com.coinffeine.common.protocol.TransactionSignatureUtils
import com.coinffeine.common.protocol.messages.PublicMessage

case class RefundTxSignatureResponse(
  exchangeId: String,
  refundSignature: TransactionSignature
) extends PublicMessage {

  override def equals(that: Any) = that match {
    case rep: RefundTxSignatureResponse => (rep.exchangeId == exchangeId) &&
      TransactionSignatureUtils.equals(rep.refundSignature, refundSignature)
    case _ => false
  }
}
