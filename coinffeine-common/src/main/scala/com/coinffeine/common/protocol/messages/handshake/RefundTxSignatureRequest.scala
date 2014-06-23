package com.coinffeine.common.protocol.messages.handshake

import com.coinffeine.common.bitcoin.ImmutableTransaction
import com.coinffeine.common.protocol.messages.PublicMessage

case class RefundTxSignatureRequest(
  exchangeId : String,
  refundTx: ImmutableTransaction
) extends PublicMessage
