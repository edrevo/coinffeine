package com.bitwise.bitmarket.common.protocol

import com.google.bitcoin.core.Sha256Hash

case class CommitmentNotification (
  exchangeId: String,
  buyerTxId: Sha256Hash,
  sellerTxId: Sha256Hash
)
