package com.bitwise.bitmarket.common.protocol.bitcoin

import com.bitwise.bitmarket.common.protocol.TransactionSerialization
import com.google.bitcoin.core.{NetworkParameters, Transaction}
import com.google.bitcoin.crypto.TransactionSignature

/** Canonical serialization method for Bitcoin transactions.
  *
  * This is the canonical serialization method used in Bitcoin protocol.
  */
class CanonicalTransactionSerialization(
    networkParams: NetworkParameters) extends TransactionSerialization {

  override def deserializeTransaction(bytes: Array[Byte]): Transaction =
    new Transaction(networkParams, bytes)

  override def deserializeTransactionSignature(bytes: Array[Byte]): TransactionSignature = ???

  override def serializeTransaction(tx: Transaction): Array[Byte] = tx.bitcoinSerialize()

  override def serializeTransactionSignature(sig: TransactionSignature): Array[Byte] = ???
}
