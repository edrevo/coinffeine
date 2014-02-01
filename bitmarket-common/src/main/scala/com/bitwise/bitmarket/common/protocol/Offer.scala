package com.bitwise.bitmarket.common.protocol

import com.bitwise.bitmarket.common.PeerConnection
import com.bitwise.bitmarket.common.currency.{FiatAmount, BtcAmount}
import com.bitwise.bitmarket.common.protorpc.{Callbacks, PeerSession}
import com.bitwise.bitmarket.common.protocol.protobuf.{
  BitmarketProtobuf => proto, ProtobufConversions}

case class Offer(
    id: String,
    sequenceNumber: Int,
    fromId: PeerId,
    fromConnection: PeerConnection,
    amount: BtcAmount,
    bitcoinPrice: FiatAmount) {

  override def toString = s"offer with ID $id from $fromId of $amount ($bitcoinPrice per BTC)"
}

object Offer {

  implicit val Write = new MessageSend[Offer] {

    def sendAsProto(msg: Offer, session: PeerSession) = {
      val stub = proto.PeerService.newStub(session.channel)
      stub.publish(
        session.controller,
        ProtobufConversions.toProtobuf(msg),
        Callbacks.noop[proto.PublishResponse])
    }
  }
}

