package com.coinffeine.common.protocol

import com.google.bitcoin.core.Transaction
import com.google.bitcoin.crypto.TransactionSignature
import org.scalatest.Assertions

class FakeTransactionSerialization(
    transactions: Seq[Transaction],
    signatures: Seq[TransactionSignature]) extends TransactionSerialization with Assertions {

  private val txMap = transactions.map(tx => (tx, tx.bitcoinSerialize())).toMap
  private val sigMap = signatures.map(sig => (sig, sig.encodeToBitcoin())).toMap

  override def deserializeTransactionSignature(bytes: Array[Byte]) = sigMap.collectFirst {
    case (sig, `bytes`) => sig
  }.getOrElse(fail("cannot deserialize unknown transaction signature"))

  override def deserializeTransaction(bytes: Array[Byte]) = txMap.collectFirst {
    case (tx, `bytes`) => tx
  }.getOrElse(fail("cannot deserialize unknown transaction"))

  override def serializeTransactionSignature(sig: TransactionSignature) =
    sigMap.getOrElse(sig, fail("cannot serialize unknown transaction signature"))

  override def serializeTransaction(tx: Transaction) =
    txMap.getOrElse(tx, fail("cannot serialize unknown transaction"))
}

