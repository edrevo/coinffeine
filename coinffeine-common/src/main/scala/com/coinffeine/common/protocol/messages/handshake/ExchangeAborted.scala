package com.coinffeine.common.protocol.messages.handshake

import com.coinffeine.common.protocol.messages.PublicMessage

case class ExchangeAborted (
  exchangeId: String,
  reason: String
) extends PublicMessage
