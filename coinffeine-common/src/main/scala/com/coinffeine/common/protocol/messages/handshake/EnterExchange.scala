package com.coinffeine.common.protocol.messages.handshake

import com.coinffeine.common.protocol.messages.MessageSend
import com.coinffeine.common.protorpc.PeerSession

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
