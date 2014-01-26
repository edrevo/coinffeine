package com.bitwise.bitmarket.common.protocol

case class CommitmentNotification (
  exchangeId: String,
  buyerTxId: String,
  sellerTxId: String
)
