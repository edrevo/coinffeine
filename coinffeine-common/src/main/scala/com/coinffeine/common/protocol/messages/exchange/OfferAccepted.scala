package com.coinffeine.common.protocol.messages.exchange

import com.google.bitcoin.crypto.TransactionSignature
import com.coinffeine.common.protocol.messages.MessageSend
import com.coinffeine.common.protorpc.PeerSession

case class OfferAccepted(exchangeId: String, signature: TransactionSignature)

object OfferAccepted {
  implicit val Write = new MessageSend[OfferAccepted] {
    override def sendAsProto(msg: OfferAccepted, session: PeerSession) = ???
  }
}
