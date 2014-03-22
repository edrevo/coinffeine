package com.coinffeine.common.protocol.messages.handshake

import com.coinffeine.common.protocol.messages.PublicMessage

case class RefundTxSignatureRequest(
  exchangeId : String,
  refundTx: Array[Byte]
) extends PublicMessage {

  override def equals(what: Any) = what match {
    case RefundTxSignatureRequest(id, tx) => (id == exchangeId) && (refundTx.deep == tx.deep)
    case _ => false
  }
}
