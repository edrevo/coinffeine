package com.coinffeine.common.protocol.messages.handshake

import com.coinffeine.common.bitcoin.MutableTransaction
import com.coinffeine.common.protocol.messages.PublicMessage

case class RefundTxSignatureRequest(
  exchangeId : String,
  refundTx: MutableTransaction
) extends PublicMessage
