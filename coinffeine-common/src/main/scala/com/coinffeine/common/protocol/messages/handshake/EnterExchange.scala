package com.coinffeine.common.protocol.messages.handshake

import com.coinffeine.common.protocol.messages.PublicMessage

case class EnterExchange(
  exchangeId: String,
  commitmentTransaction: Array[Byte]
) extends PublicMessage {

  override def equals(what: Any) = what match {
    case EnterExchange(id, tx) => (exchangeId == id) && (commitmentTransaction.deep == tx.deep)
  }
}
