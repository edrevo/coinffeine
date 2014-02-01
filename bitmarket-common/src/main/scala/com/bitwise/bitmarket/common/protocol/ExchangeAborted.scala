package com.bitwise.bitmarket.common.protocol

import com.bitwise.bitmarket.common.protorpc.PeerSession

case class ExchangeAborted (
  exchangeId: String,
  reason: String
)

object ExchangeAborted {

  implicit val Write = new MessageSend[ExchangeAborted] {

    def sendAsProto(msg: ExchangeAborted, session: PeerSession) = ???
  }
}

