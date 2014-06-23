package com.coinffeine.common.protocol.messages.handshake

import com.coinffeine.common.bitcoin.MutableTransaction
import com.coinffeine.common.protocol.messages.PublicMessage

case class ExchangeCommitment(
  exchangeId: String,
  commitmentTransaction: MutableTransaction
) extends PublicMessage
