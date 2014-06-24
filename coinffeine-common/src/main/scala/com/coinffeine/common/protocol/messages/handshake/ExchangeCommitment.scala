package com.coinffeine.common.protocol.messages.handshake

import com.coinffeine.common.bitcoin.ImmutableTransaction
import com.coinffeine.common.protocol.messages.PublicMessage
import com.coinffeine.common.protocol.messages.exchange.ExchangeId

case class ExchangeCommitment(
  exchangeId: ExchangeId,
  commitmentTransaction: ImmutableTransaction
) extends PublicMessage
