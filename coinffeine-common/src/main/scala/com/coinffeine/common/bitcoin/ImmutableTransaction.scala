package com.coinffeine.common.bitcoin

/** Wrapper for bitcoinj transactions.
  *
  * As bitcoinj transactions are mutable we need a source of fresh objects to keep our sanity.
  * This class adds some niceties such as a proper string conversion an a syntax similar to
  * {{{Future { ... } }}}.
  */
class ImmutableTransaction(private val bytes: Array[Byte], private val network: Network) {

  def this(tx: MutableTransaction) = this(tx.bitcoinSerialize(), tx.getParams)

  override def toString: String = get.toString

  def get: MutableTransaction = new MutableTransaction(network, bytes)

  override def equals(other: Any): Boolean = other match {
    case that: ImmutableTransaction => bytes.sameElements(that.bytes) && network == that.network
    case _ => false
  }

  override def hashCode(): Int =
    bytes.foldLeft(network.hashCode())((accum, elem) => 31 * accum + elem.hashCode())
}

object ImmutableTransaction {
  def apply(tx: MutableTransaction) = new ImmutableTransaction(tx)
}
