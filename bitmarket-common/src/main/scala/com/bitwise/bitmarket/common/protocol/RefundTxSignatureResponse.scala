package com.bitwise.bitmarket.common.protocol

import com.google.bitcoin.crypto.TransactionSignature

import com.bitwise.bitmarket.common.protorpc.PeerSession

case class RefundTxSignatureResponse(
  exchangeId: String,
  refundSignature: TransactionSignature
)

object RefundTxSignatureResponse {

  implicit val Write = new MessageSend[RefundTxSignatureResponse] {

    def sendAsProto(msg: RefundTxSignatureResponse, session: PeerSession) = ???
  }
}

