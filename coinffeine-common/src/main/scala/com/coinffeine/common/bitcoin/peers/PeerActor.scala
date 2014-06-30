package com.coinffeine.common.bitcoin.peers

import akka.actor.ActorRef

import com.coinffeine.common.bitcoin.ImmutableTransaction

/** A PeerActor handles connections to other peers in the bitcoin network and can:
  *
  * - Return a reference to the BlockchainActor that contains the blockchain derived from the peers
  * - Broadcast a transaction to the peers
  */
object PeerActor {
  /** A request for the actor to publish the transaction to its peers so it eventually
    * gets confirmed in the blockchain.
    *
    * @param tx The transaction to be broadcast
    */
  case class PublishTransaction(tx: ImmutableTransaction)

  /** A message sent by the peer actor to confirm that a PublishTransaction request has been
    * completed successfully.
    *
    * @param originalTx The transaction that was used in the PublishTransaction request
    * @param broadcastTx The transaction that was broadcast. This transaction is the canonical
    *                    version of originalTx, which should be used in further requests to the
    *                    blockchain (for example when requesting a certain number of confirmations)
    */
  case class TransactionPublished(
    originalTx: ImmutableTransaction, broadcastTx: ImmutableTransaction)

  /** A message sent by the blockchain actor to notify that a PublishTransaction request could not
    * be completed successfully
    */
  case class TransactionNotPublished(tx: ImmutableTransaction, cause: Throwable)

  /** A request to the actor to retrieve the blockchain actor that contains the blockchain that
    * contains the blocks announced by the peers this actor knows about
    */
  case object RetrieveBlockchainActor

  /** The response to the RetrieveBlockchainActor request */
  case class BlockchainActorReference(ref: ActorRef)

  case object NoPeersAvailable extends RuntimeException("There are no peers available")
}
