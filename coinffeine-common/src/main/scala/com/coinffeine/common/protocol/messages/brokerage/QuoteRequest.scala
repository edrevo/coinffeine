package com.coinffeine.common.protocol.messages.brokerage

import java.util.Currency

import com.coinffeine.common.protocol.messages.MessageSend
import com.coinffeine.common.protorpc.PeerSession

/** Used to ask about the current quote of bitcoin traded in a given currency */
case class QuoteRequest(currency: Currency)

object QuoteRequest {
  implicit val Write = new MessageSend[QuoteRequest] {
    override def sendAsProto(msg: QuoteRequest, session: PeerSession) = ???
  }
}
