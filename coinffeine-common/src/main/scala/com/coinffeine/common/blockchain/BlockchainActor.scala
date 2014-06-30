package com.coinffeine.common.blockchain

import akka.actor.Props

import com.coinffeine.common.bitcoin.{ImmutableTransaction, Hash, PublicKey}

/** A BlockchainActor keeps a blockchain and can:
  *
  * - Notify when a transaction reaches a number of confirmations.
  * - Return the transaction associated with a hash
  */
object BlockchainActor {

  /** A message sent to the blockchain actor requesting to watch for transactions on the given
    * public key.
    */
  case class WatchPublicKey(publicKey: PublicKey)

  /** A message sent to the blockchain actor requesting to watch for confirmation of the
    * given block.
    *
    * The blockchain actor will send either `TransactionConfirmed` or `TransactionRejected`
    * as response.
    */
  case class WatchTransactionConfirmation(transactionHash: Hash, confirmations: Int)

  /** A message sent by the blockchain actor to notify that the transaction has reached the
    * requested number of confirmations. */
  case class TransactionConfirmed(transactionHash: Hash, confirmations: Int)

  /** A message sent by the blockchain actor to notify that the transaction has been rejected. */
  case class TransactionRejected(transactionHash: Hash)

  /** A message sent to the blockchain actor requesting to be notified when the best block in the
    * blockchain reaches a specified height.
    */
  case class WatchBlockchainHeight(height: Long)

  /** A message sent by the blockchain actor to notify that the blockchain has reached a certain
    * height.
    */
  case class BlockchainHeightReached(height: Long)

  /** A message sent to the blockchain actor to retrieve a transaction from its hash.
    *
    * The blockchain actor will send either a `TransactionFound` or `TransactionNotFound`
    * as response.
    */
  case class RetrieveTransaction(hash: Hash)

  /** A message sent by the blockchain actor to indicate a transaction was found in the blockchain
    * for the given hash.
    */
  case class TransactionFound(hash: Hash, tx: ImmutableTransaction)

  /** A message sent by the blockchain actor to indicate a transaction was not found in the
    * blockchain for the given hash.
    */
  case class TransactionNotFound(hash: Hash)

  trait Component {
    def blockchainActorProps(): Props
  }
}
