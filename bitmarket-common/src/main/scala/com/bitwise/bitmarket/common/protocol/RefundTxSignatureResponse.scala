package com.bitwise.bitmarket.common.protocol

import com.google.bitcoin.crypto.TransactionSignature

case class RefundTxSignatureResponse (
  exchangeId : String,
  refundTxSignature: TransactionSignature
)
