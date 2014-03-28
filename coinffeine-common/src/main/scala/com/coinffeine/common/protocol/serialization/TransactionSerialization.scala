package com.coinffeine.common.protocol.serialization

import com.google.bitcoin.core.{Transaction, NetworkParameters}
import com.google.bitcoin.crypto.TransactionSignature
import com.google.protobuf.ByteString

private[serialization] class TransactionSerialization(network: NetworkParameters) {

  def deserializeSignature(byteString: ByteString): TransactionSignature =
    TransactionSignature.decodeFromBitcoin(byteString.toByteArray,
      TransactionSerialization.RequireCanonical)

  def deserializeTransaction(byteString: ByteString): Transaction =
    new Transaction(network, byteString.toByteArray)

  def serialize(sig: TransactionSignature): ByteString = ByteString.copyFrom(sig.encodeToBitcoin())

  def serialize(tx: Transaction): ByteString = ByteString.copyFrom(tx.bitcoinSerialize())
}

private object TransactionSerialization {
  /** Reject deserialization of non-canonical signatures */
  private val RequireCanonical = true
}
