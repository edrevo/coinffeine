package com.coinffeine.common.protocol.messages.handshake

import com.coinffeine.common.bitcoin.ImmutableTransaction
import com.coinffeine.common.protocol.messages.PublicMessage
import com.coinffeine.common.protocol.messages.exchange.ExchangeId

case class RefundTxSignatureRequest(
  exchangeId: ExchangeId,
  refundTx: ImmutableTransaction
) extends PublicMessage
