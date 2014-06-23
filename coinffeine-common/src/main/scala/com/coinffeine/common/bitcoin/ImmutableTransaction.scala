package com.coinffeine.common.bitcoin

/** Wrapper for bitcoinj transactions.
  *
  * As bitcoinj transactions are mutable we need a source of fresh objects to keep our sanity.
  * This class adds some niceties such as a proper string conversion an a syntax similar to
  * {{{Future { ... } }}}.
  */
private[impl] class ImmutableTransaction(origTx: MutableTransaction) {

  private val network: Network = origTx.getParams
  private val bytes: Array[Byte] = origTx.bitcoinSerialize()

  override def toString: String = get.toString

  def get: MutableTransaction = new MutableTransaction(network, bytes)
}

private[impl] object ImmutableTransaction {

  def apply(tx: MutableTransaction) = new ImmutableTransaction(tx)
}
