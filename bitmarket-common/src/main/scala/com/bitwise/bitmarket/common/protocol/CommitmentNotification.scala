package com.bitwise.bitmarket.common.protocol

import com.google.bitcoin.core.Sha256Hash

import com.bitwise.bitmarket.common.protorpc.PeerSession

case class CommitmentNotification(
  exchangeId: String,
  buyerTxId: Sha256Hash,
  sellerTxId: Sha256Hash
)

object CommitmentNotification {

  implicit val Write = new MessageSend[CommitmentNotification] {

    def sendAsProto(msg: CommitmentNotification, session: PeerSession) = ???
  }
}
