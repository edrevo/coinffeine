package com.coinffeine.common.protocol.messages.handshake

import com.coinffeine.common.protocol.messages.PublicMessage

case class ExchangeRejection (
  exchangeId: String,
  reason: String
) extends PublicMessage
