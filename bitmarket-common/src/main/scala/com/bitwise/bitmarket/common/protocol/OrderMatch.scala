package com.bitwise.bitmarket.common.protocol

import com.bitwise.bitmarket.common.currency.{FiatAmount, BtcAmount}
import com.bitwise.bitmarket.common.protorpc.{Callbacks, PeerSession}
import com.bitwise.bitmarket.common.protocol.protobuf.{
  ProtobufConversions, BitmarketProtobuf => proto}

/** Represents a coincidence of desires of both a buyer and a seller */
case class OrderMatch(
    orderMatchId: String,
    amount: BtcAmount,
    price: FiatAmount,
    buyer: String,
    seller: String
)

object OrderMatch {

  implicit val Write = new MessageSend[OrderMatch] {

    def sendAsProto(msg: OrderMatch, session: PeerSession) = {
      val stub = proto.PeerService.newStub(session.channel)
      stub.notifyMatch(
        session.controller,
        ProtobufConversions.toProtobuf(msg),
        Callbacks.noop[proto.Void])
    }
  }
}
