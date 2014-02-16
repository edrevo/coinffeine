package com.coinffeine.common.protocol

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
