package com.bitwise.bitmarket.common.protocol

import com.bitwise.bitmarket.common.protorpc.PeerSession

/**
 * Represents the ask/bid cross information from two peers.
 *
 * @param exchangeId ID of Exchange. Is used in all messages on this exchange.
 * @param cross OrderMatch object which contains all information about this ask/bid cross.
 */
case class CrossNotification (
  exchangeId: String,
  cross: OrderMatch
)

object CrossNotification {

  implicit val Write = new MessageSend[CrossNotification] {

    def sendAsProto(msg: CrossNotification, session: PeerSession) = ???
  }
}
