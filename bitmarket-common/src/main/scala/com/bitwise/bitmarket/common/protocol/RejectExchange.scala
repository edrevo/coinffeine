package com.bitwise.bitmarket.common.protocol

import com.bitwise.bitmarket.common.protorpc.PeerSession

case class RejectExchange (
  exchangeId: String,
  reason: String
)

object RejectExchange {

  implicit val Write = new MessageSend[RejectExchange] {

    def sendAsProto(msg: RejectExchange, session: PeerSession) = ???
  }
}
