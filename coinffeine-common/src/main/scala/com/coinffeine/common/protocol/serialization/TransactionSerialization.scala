package com.coinffeine.common.protocol.serialization

import com.google.protobuf.ByteString

import com.coinffeine.common.bitcoin.{MutableTransaction, Network, TransactionSignature}

private[serialization] class TransactionSerialization(network: Network) {

  def deserializeSignature(byteString: ByteString): TransactionSignature =
    TransactionSignature.decode(byteString.toByteArray, TransactionSerialization.RequireCanonical)

  def deserializeTransaction(byteString: ByteString): MutableTransaction =
    new MutableTransaction(network, byteString.toByteArray)

  def serialize(sig: TransactionSignature): ByteString = ByteString.copyFrom(sig.encodeToBitcoin())

  def serialize(tx: MutableTransaction): ByteString = ByteString.copyFrom(tx.bitcoinSerialize())
}

private object TransactionSerialization {
  /** Reject deserialization of non-canonical signatures */
  private val RequireCanonical = true
}
