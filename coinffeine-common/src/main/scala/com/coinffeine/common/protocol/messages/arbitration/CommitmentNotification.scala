package com.coinffeine.common.protocol.messages.arbitration

import com.coinffeine.common.bitcoin.Hash
import com.coinffeine.common.protocol.messages.PublicMessage

case class CommitmentNotification(
  exchangeId: String,
  buyerTxId: Hash,
  sellerTxId: Hash
) extends PublicMessage
