package com.coinffeine.common.protocol.messages.handshake

import com.google.bitcoin.core.Transaction

import com.coinffeine.common.protocol.messages.PublicMessage

case class EnterExchange(
  exchangeId: String,
  commitmentTransaction: Transaction
) extends PublicMessage
