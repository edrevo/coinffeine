package com.coinffeine.common.protocol.messages.exchange

import com.coinffeine.common.protocol.messages.MessageSend
import com.coinffeine.common.protorpc.PeerSession

case class PaymentProof(exchangeId: String, paymentId: String)

object PaymentProof {
  implicit val Write = new MessageSend[PaymentProof] {
    override def sendAsProto(msg: PaymentProof, session: PeerSession) = ???
  }
}
