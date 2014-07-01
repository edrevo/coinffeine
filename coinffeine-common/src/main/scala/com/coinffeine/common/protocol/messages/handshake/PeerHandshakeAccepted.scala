package com.coinffeine.common.protocol.messages.handshake

import com.coinffeine.common.bitcoin.TransactionSignature
import com.coinffeine.common.exchange.Exchange
import com.coinffeine.common.protocol.TransactionSignatureUtils
import com.coinffeine.common.protocol.messages.PublicMessage

case class PeerHandshakeAccepted(exchangeId: Exchange.Id, refundSignature: TransactionSignature)
  extends PublicMessage {

  override def equals(that: Any) = that match {
    case rep: PeerHandshakeAccepted => (rep.exchangeId == exchangeId) &&
      TransactionSignatureUtils.equals(rep.refundSignature, refundSignature)
    case _ => false
  }

  override def hashCode(): Int =
    31 * exchangeId.hashCode() + TransactionSignatureUtils.hashCode(refundSignature)
}
