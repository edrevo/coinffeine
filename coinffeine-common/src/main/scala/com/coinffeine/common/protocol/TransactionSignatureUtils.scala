package com.coinffeine.common.protocol

import com.google.bitcoin.crypto.TransactionSignature

object TransactionSignatureUtils {
  def equals(s1: TransactionSignature, s2: TransactionSignature): Boolean = (s1, s2) match {
    case (null, null) => true
    case (null, _) => false
    case (_, null) => false
    case _ => s1.encodeToBitcoin().sameElements(s2.encodeToBitcoin())
  }
}
