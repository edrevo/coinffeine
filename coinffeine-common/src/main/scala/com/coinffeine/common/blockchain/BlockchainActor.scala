package com.coinffeine.common.blockchain

import akka.actor.Props

import com.coinffeine.common.bitcoin.{Hash, MutableTransaction}

/** A BlockchainActor keeps a blockchain and can:
  *
  * - Notify when a transaction reaches a number of confirmations.
  * - Return the transaction associated with a hash
  */
object BlockchainActor {

  /** The blockchain actor will send either `TransactionConfirmed` or `TransactionRejected`
    * to whoever sends this message.
    */
  case class NotifyWhenConfirmed(transactionHash: Hash, confirmations: Int)

  /** Sent when the TX reaches the requested number of confirmations. */
  case class TransactionConfirmed(transactionHash: Hash, confirmations: Int)

  /** Sent if the TX becomes invalid (i.e. input funds have been spent otherwise) while
    * waiting for it to be confirmed.
    */
  case class TransactionRejected(transactionHash: Hash)

  case class PublishTransaction(transaction: MutableTransaction)

  /** The blockchain actor will send either a `TransactionFor` or `TransactionNotFoundWith`
    * to whoever sends this message.
    */
  case class GetTransactionFor(hash: Hash)

  /** Sent if a transaction is found in the blockchain for the given hash */
  case class TransactionFor(hash: Hash, tx: MutableTransaction)

  /** Sent if there is no transaction in the blockchain for the given hash */
  case class TransactionNotFoundWith(hash: Hash)

  trait Component {
    def blockchainActorProps(): Props
  }
}
