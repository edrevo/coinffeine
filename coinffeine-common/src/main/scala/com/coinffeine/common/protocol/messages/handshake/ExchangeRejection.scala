package com.coinffeine.common.protocol.messages.handshake

import com.coinffeine.common.exchange.Exchange
import com.coinffeine.common.protocol.messages.PublicMessage

case class ExchangeRejection (
  exchangeId: Exchange.Id,
  reason: String
) extends PublicMessage
