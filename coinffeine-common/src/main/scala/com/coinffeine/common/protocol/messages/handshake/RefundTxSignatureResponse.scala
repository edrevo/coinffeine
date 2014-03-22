package com.coinffeine.common.protocol.messages.handshake

import com.google.bitcoin.crypto.TransactionSignature

import com.coinffeine.common.protocol.messages.PublicMessage

case class RefundTxSignatureResponse(
  exchangeId: String,
  refundSignature: TransactionSignature
) extends PublicMessage {

  override def equals(that: Any) = that match {
    case rep: RefundTxSignatureResponse =>
      (rep.exchangeId == exchangeId) && equals(rep.refundSignature, refundSignature)
    case _ => false
  }

  private def equals(s1: TransactionSignature, s2: TransactionSignature): Boolean =
    (s1.encodeToBitcoin(), s2.encodeToBitcoin()) match {
      case (null, null) => true
      case (null, _) => false
      case (_, null) => false
      case (b1: Array[Byte], b2: Array[Byte]) => b1.deep == b2.deep
    }
}
