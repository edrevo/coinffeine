package com.coinffeine.common.protocol.messages.handshake

import com.coinffeine.common.protocol.messages.PublicMessage
import com.coinffeine.common.protocol.messages.exchange.ExchangeId

case class ExchangeRejection (
  exchangeId: ExchangeId,
  reason: String
) extends PublicMessage
