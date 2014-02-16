package com.bitwise.bitmarket.common.protocol

import com.bitwise.bitmarket.common.protorpc.PeerSession

case class EnterExchange(
  exchangeId: String,
  commitmentTransaction: Array[Byte]
) {

  override def equals(what: Any) = what match {
    case EnterExchange(id, tx) => (exchangeId == id) && (commitmentTransaction.deep == tx.deep)
  }
}

object EnterExchange {

  implicit val Write = new MessageSend[EnterExchange] {

    def sendAsProto(msg: EnterExchange, session: PeerSession) = ???
  }
}
