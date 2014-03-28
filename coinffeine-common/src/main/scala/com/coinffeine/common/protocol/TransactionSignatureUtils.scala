package com.coinffeine.common.protocol

import com.google.bitcoin.crypto.TransactionSignature

object TransactionSignatureUtils {
  def equals(s1: TransactionSignature, s2: TransactionSignature): Boolean =
    (s1.encodeToBitcoin(), s2.encodeToBitcoin()) match {
      case (null, null) => true
      case (null, _) => false
      case (_, null) => false
      case (b1: Array[Byte], b2: Array[Byte]) => b1.sameElements(b2)
    }
}
