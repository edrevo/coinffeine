package com.bitwise.bitmarket.common.protocol

import com.google.bitcoin.core.Transaction

import com.bitwise.bitmarket.common.protorpc.PeerSession

case class EnterExchange(
  exchangeId: String,
  commitmentTransaction: Transaction
)

object EnterExchange {

  implicit val Write = new MessageSend[EnterExchange] {

    def sendAsProto(msg: EnterExchange, session: PeerSession) = ???
  }
}
