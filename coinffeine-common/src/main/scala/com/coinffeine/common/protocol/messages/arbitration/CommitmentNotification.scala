package com.coinffeine.common.protocol.messages.arbitration

import com.google.bitcoin.core.Sha256Hash

import com.coinffeine.common.protocol.messages.MessageSend
import com.coinffeine.common.protorpc.PeerSession

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
