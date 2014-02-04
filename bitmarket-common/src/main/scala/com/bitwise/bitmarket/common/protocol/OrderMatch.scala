package com.bitwise.bitmarket.common.protocol

import com.bitwise.bitmarket.common.PeerConnection
import com.bitwise.bitmarket.common.currency.{FiatAmount, BtcAmount}
import com.bitwise.bitmarket.common.protorpc.{Callbacks, PeerSession}
import com.bitwise.bitmarket.common.protocol.protobuf.{BitmarketProtobuf => proto, ProtoMapping}
import com.bitwise.bitmarket.common.protocol.protobuf.DefaultProtoMappings._

/** Represents a coincidence of desires of both a buyer and a seller */
case class OrderMatch(
    exchangeId: String,
    amount: BtcAmount,
    price: FiatAmount,
    buyer: PeerConnection,
    seller: PeerConnection
)

object OrderMatch {

  implicit val Write = new MessageSend[OrderMatch] {

    def sendAsProto(msg: OrderMatch, session: PeerSession) = {
      val stub = proto.PeerService.newStub(session.channel)
      stub.notifyMatch(
        session.controller,
        ProtoMapping.toProtobuf(msg),
        Callbacks.noop[proto.Void])
    }
  }
}
