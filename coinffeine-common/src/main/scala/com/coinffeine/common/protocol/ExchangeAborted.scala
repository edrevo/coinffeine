package com.coinffeine.common.protocol

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

