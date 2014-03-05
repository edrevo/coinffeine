package com.coinffeine.common.protocol.messages.handshake

import com.coinffeine.common.protocol.messages.MessageSend
import com.coinffeine.common.protorpc.PeerSession

case class ExchangeAborted (
  exchangeId: String,
  reason: String
)

object ExchangeAborted {

  implicit val Write = new MessageSend[ExchangeAborted] {

    def sendAsProto(msg: ExchangeAborted, session: PeerSession) = ???
  }
}

