package com.bitwise.bitmarket.common.protocol

import com.google.bitcoin.core.Transaction
import com.google.bitcoin.crypto.TransactionSignature

/** An object able to serialize and deserialize bitcoin transactions. */
trait TransactionSerialization {

  def serializeTransaction(tx: Transaction): Array[Byte]

  def serializeTransactionSignature(sig: TransactionSignature): Array[Byte]

  def deserializeTransaction(bytes: Array[Byte]): Transaction

  def deserializeTransactionSignature(bytes: Array[Byte]): TransactionSignature
}

object TransactionSerialization {
  trait Component {
    def transactionSerialization: TransactionSerialization
  }
}
