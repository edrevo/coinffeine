package com.coinffeine.common.exchange.impl

import com.google.bitcoin.core.{NetworkParameters, Transaction}

/** Wrapper for bitcoinj transactions.
  *
  * As bitcoinj transactions are mutable we need a source of fresh objects to keep our sanity.
  * This class adds some niceties such as a proper string conversion an a syntax similar to
  * {{{Future { ... } }}}.
  */
class ImmutableTransaction(origTx: Transaction) {

  private val network: NetworkParameters = origTx.getParams
  private val bytes: Array[Byte] = origTx.bitcoinSerialize()

  override def toString: String = get.toString

  def get: Transaction = new Transaction(network, bytes)
}

object ImmutableTransaction {

  def apply(tx: Transaction) = new ImmutableTransaction(tx)
}
