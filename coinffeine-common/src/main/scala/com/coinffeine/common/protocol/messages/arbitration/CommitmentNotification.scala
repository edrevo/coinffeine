package com.coinffeine.common.protocol.messages.arbitration

import com.coinffeine.common.bitcoin.Hash
import com.coinffeine.common.exchange.{Both, Exchange}
import com.coinffeine.common.protocol.messages.PublicMessage

case class CommitmentNotification(
  exchangeId: Exchange.Id,
  bothCommitments: Both[Hash]
) extends PublicMessage
