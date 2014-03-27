package com.coinffeine.common.protocol.messages.exchange

import com.google.bitcoin.core.Transaction

import com.coinffeine.common.protocol.messages.MessageSend
import com.coinffeine.common.protorpc.PeerSession

case class NewOffer(exchangeId: String, offer: Transaction)

object NewOffer {
  implicit val Write = new MessageSend[NewOffer] {
    override def sendAsProto(msg: NewOffer, session: PeerSession) = ???
  }
}
