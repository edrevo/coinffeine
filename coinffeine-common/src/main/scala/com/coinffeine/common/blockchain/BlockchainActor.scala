package com.coinffeine.common.blockchain

import akka.actor.Props
import com.google.bitcoin.core.{Transaction, Sha256Hash}

/** A BlockchainActor keeps a blockchain and can notify when a transaction reaches a number of
  * confirmations.
  */
object BlockchainActor {

  /** The blockchain actor will send either `TransactionConfirmed` or `TransactionRejected`
    * to whoever sends this message.
    */
  case class NotifyWhenConfirmed(transactionHash: Sha256Hash, confirmations: Int)

  /** Sent when the TX reaches the requested number of confirmations. */
  case class TransactionConfirmed(transactionHash: Sha256Hash, confirmations: Int)

  /** Send if the TX becomes invalid (i.e. input funds have been spent otherwise) while
    * waiting for it to be confirmed.
    */
  case class TransactionRejected(transactionHash: Sha256Hash)

  case class PublishTransaction(transaction: Transaction)

  trait Component {
    def blockchainActorProps(): Props
  }
}
