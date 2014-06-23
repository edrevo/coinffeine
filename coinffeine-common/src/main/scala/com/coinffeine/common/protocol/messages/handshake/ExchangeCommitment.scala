package com.coinffeine.common.protocol.messages.handshake

import com.coinffeine.common.bitcoin.ImmutableTransaction
import com.coinffeine.common.protocol.messages.PublicMessage

case class ExchangeCommitment(
  exchangeId: String,
  commitmentTransaction: ImmutableTransaction
) extends PublicMessage
