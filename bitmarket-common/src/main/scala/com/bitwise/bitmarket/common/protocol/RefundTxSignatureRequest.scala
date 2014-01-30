package com.bitwise.bitmarket.common.protocol

import com.google.bitcoin.core.Transaction

import com.bitwise.bitmarket.common.protorpc.PeerSession

case class RefundTxSignatureRequest(
  exchangeId : String,
  refundTx: Transaction
)

object RefundTxSignatureRequest {

  implicit val Write = new MessageSend[RefundTxSignatureRequest] {

    def sendAsProto(msg: RefundTxSignatureRequest, session: PeerSession) = ???
  }
}
