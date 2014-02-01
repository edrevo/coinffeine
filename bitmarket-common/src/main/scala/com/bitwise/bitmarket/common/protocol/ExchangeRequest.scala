package com.bitwise.bitmarket.common.protocol

import com.bitwise.bitmarket.common.PeerConnection
import com.bitwise.bitmarket.common.currency.BtcAmount
import com.bitwise.bitmarket.common.protorpc.{Callbacks, PeerSession}
import com.bitwise.bitmarket.common.protocol.protobuf.{
  ProtobufConversions, BitmarketProtobuf => proto}

case class ExchangeRequest(
    exchangeId: String,
    fromId: PeerId,
    fromConnection: PeerConnection,
    amount: BtcAmount
)

object ExchangeRequest {

  implicit val Write = new MessageSend[ExchangeRequest] {

    def sendAsProto(msg: ExchangeRequest, session: PeerSession) = {
      val stub = proto.PeerService.newStub(session.channel)
      stub.requestExchange(
        session.controller,
        ProtobufConversions.toProtobuf(msg),
        Callbacks.noop[proto.ExchangeRequestResponse])
    }
  }
}


