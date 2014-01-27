package com.bitwise.bitmarket.client

import akka.actor.Props
import com.google.bitcoin.core.Sha256Hash

/** A BlockchainActor keeps a blockchain and can notify when a transaction reaches a number of
  * confirmations.
  */
object BlockchainActor {

  case class NotifyWhenConfirmed(transactionHash: Sha256Hash, confirmations: Int)
  case class TransactionConfirmed(transactionHash: Sha256Hash, confirmations: Int)

  trait Component {
    def blockchainActorProps(): Props
  }
}
