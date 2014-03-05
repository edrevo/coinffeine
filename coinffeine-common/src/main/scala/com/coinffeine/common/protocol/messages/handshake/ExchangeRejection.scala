package com.coinffeine.common.protocol.messages.handshake

import com.coinffeine.common.protocol.messages.MessageSend
import com.coinffeine.common.protorpc.PeerSession

case class ExchangeRejection (
  exchangeId: String,
  reason: String
)

object ExchangeRejection {

  implicit val Write = new MessageSend[ExchangeRejection] {

    def sendAsProto(msg: ExchangeRejection, session: PeerSession) = ???
  }
}
