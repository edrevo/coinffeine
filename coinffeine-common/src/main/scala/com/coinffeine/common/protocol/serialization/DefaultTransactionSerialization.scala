/*
 * Telefónica Digital - Product Development and Innovation
 *
 * THIS CODE AND INFORMATION ARE PROVIDED "AS IS" WITHOUT WARRANTY OF ANY KIND,
 * EITHER EXPRESSED OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND/OR FITNESS FOR A PARTICULAR PURPOSE.
 *
 * Copyright (c) Telefónica Investigación y Desarrollo S.A.U.
 * All rights reserved.
 */

package com.coinffeine.common.protocol.serialization

import com.google.bitcoin.core.{Transaction, NetworkParameters}
import com.google.bitcoin.crypto.TransactionSignature

class DefaultTransactionSerialization(network: NetworkParameters) extends TransactionSerialization {

  override def deserializeTransactionSignature(bytes: Array[Byte]): TransactionSignature =
    TransactionSignature.decodeFromBitcoin(bytes, true)

  override def deserializeTransaction(bytes: Array[Byte]): Transaction =
    new Transaction(network, bytes)

  override def serializeTransactionSignature(sig: TransactionSignature): Array[Byte] =
    sig.encodeToBitcoin()

  override def serializeTransaction(tx: Transaction): Array[Byte] = tx.bitcoinSerialize()
}
