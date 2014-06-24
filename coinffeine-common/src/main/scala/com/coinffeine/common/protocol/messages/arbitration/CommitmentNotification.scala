package com.coinffeine.common.protocol.messages.arbitration

import com.coinffeine.common.bitcoin.Hash
import com.coinffeine.common.protocol.messages.PublicMessage
import com.coinffeine.common.protocol.messages.exchange.ExchangeId

case class CommitmentNotification(
  exchangeId: ExchangeId,
  buyerTxId: Hash,
  sellerTxId: Hash
) extends PublicMessage
