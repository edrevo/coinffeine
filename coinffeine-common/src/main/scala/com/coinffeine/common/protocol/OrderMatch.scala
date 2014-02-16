package com.coinffeine.common.protocol

import com.coinffeine.common.PeerConnection
import com.coinffeine.common.currency.{FiatAmount, BtcAmount}
import com.coinffeine.common.protorpc.{Callbacks, PeerSession}
import com.coinffeine.common.protocol.protobuf.{CoinffeineProtobuf => proto, ProtoMapping}
import com.coinffeine.common.protocol.protobuf.DefaultProtoMappings._

/** Represents a coincidence of desires of both a buyer and a seller */
case class OrderMatch(
    exchangeId: String,
    amount: BtcAmount,
    price: FiatAmount,
    buyer: PeerConnection,
    seller: PeerConnection
) {
  def participants: Set[PeerConnection] = Set(buyer, seller)
}

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
