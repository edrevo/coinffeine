package com.coinffeine.common.blockchain

import java.util
import scala.collection.JavaConversions._

import akka.actor.{ActorRef, ActorLogging, Actor, Props}
import com.google.bitcoin.core.AbstractBlockChain.NewBlockType
import com.google.bitcoin.core._
import com.google.common.util.concurrent.{FutureCallback, Futures}

import com.coinffeine.common.bitcoin._

class BlockchainActor(
    network: NetworkParameters,
    blockchain: AbstractBlockChain,
    transactionBroadcaster: TransactionBroadcaster) extends Actor with ActorLogging {

  private case class BlockIdentity(hash: Sha256Hash, height: Int)

  private case class Observation(
                                  txHash: Sha256Hash,
                                  requester: ActorRef,
                                  requiredConfirmations: Int,
                                  foundInBlock: Option[BlockIdentity] = None)

  private case class HeightNotification(
                                         height: Long,
                                         requester: ActorRef)

  private var observations: Map[Sha256Hash, Observation] = Map.empty
  private var heightNotifications: Set[HeightNotification] = Set.empty
  private val wallet: Wallet = new Wallet(network)

  private object listener extends AbstractBlockChainListener {

    override def notifyNewBestBlock(block: StoredBlock): Unit = {
      observations.foreach { case (txHash, Observation(_, req, reqConf, Some(foundInBlock))) =>
        val blockHeight = block.getHeight
        val confirmations = (blockHeight - foundInBlock.height) + 1
        if (confirmations >= reqConf) {
          log.info(
            "after new chain head {}, tx {} have {} confirmations out of {} required: " +
              "reporting to the observer",
            blockHeight, txHash, confirmations, reqConf)
          req ! BlockchainActor.TransactionConfirmed(txHash, confirmations)
          observations -= txHash
        } else {
          log.info(
            "after new chain head {}, tx {} have {} confirmations out of {} required: " +
              "still waiting for more blocks",
            blockHeight, txHash, confirmations, reqConf)
        }
      }
      heightNotifications.foreach { case notification@HeightNotification(height, req) =>
        val blockchainHeight = block.getHeight
        if (blockchainHeight >= height) {
          req ! BlockchainActor.BlockchainHeightReached(blockchainHeight)
          heightNotifications  -= notification
        }
      }
    }

    override def reorganize(splitPoint: StoredBlock, oldBlocks: util.List[StoredBlock],
                            newBlocks: util.List[StoredBlock]): Unit = {
      val seenTxs = observations.values.filter(_.foundInBlock.isDefined)
      val rejectedObservations = seenTxs.filterNot(tx =>
        newBlocks.toSeq.exists(block => block.getHeader.getHash == tx.foundInBlock.get.hash))
      rejectedObservations.foreach { obs =>
        log.info("tx {} is lost in blockchain reorganization; reporting to the observer", obs.txHash)
        obs.requester ! BlockchainActor.TransactionRejected(obs.txHash)
        observations -= obs.txHash
      }
      /* It seems to be a bug in Bitcoinj that causes the newBlocks list to be in an arbitrary
       * order although the Javadoc of BlockChainListener says it follows a top-first order.
       * Thus, we have to calculate the highest block from the list to determine that's the
       * new blockchain head.
       */
      val newChainHead = newBlocks.maxBy(_.getHeight)
      notifyNewBestBlock(newChainHead)
    }

    override def isTransactionRelevant(tx: Transaction): Boolean = observations.contains(tx.getHash)

    override def receiveFromBlock(tx: Transaction, block: StoredBlock,
                                  blockType: NewBlockType, relativityOffset: Int): Unit = {
      val txHash = tx.getHash
      val txHeight= block.getHeight
      observations.get(txHash) match {
        case Some(obs) =>
          log.info("tx {} found in block {}: waiting for {} confirmations",
            txHash, txHeight, obs.requiredConfirmations)
          observations += txHash -> obs.copy(
            foundInBlock = Some(BlockIdentity(block.getHeader.getHash, block.getHeight)))
        case None =>
          log.warning("tx {} received but not relevant (not being observed)", txHash)
      }
    }

    override def notifyTransactionIsInBlock(
                                             txHash: Sha256Hash, block: StoredBlock,
                                             blockType: NewBlockType, relativityOffset: Int): Boolean = observations.contains(txHash)
  }

  blockchain.addListener(listener, context.dispatcher)
  blockchain.addWallet(wallet)

  override def receive: Receive = {
    case BlockchainActor.WatchPublicKey(key) =>
      wallet.addKey(key)
    case req @ BlockchainActor.WatchTransactionConfirmation(txHash, confirmations) =>
      observations += txHash -> Observation(txHash, sender(), confirmations)
    case BlockchainActor.RetrieveTransaction(txHash) =>
      transactionFor(txHash) match {
        case Some(tx) => sender ! BlockchainActor.TransactionFound(txHash, ImmutableTransaction(tx))
        case None => sender ! BlockchainActor.TransactionNotFound(txHash)
      }
    case BlockchainActor.PublishTransaction(tx) =>
      broadcastTransaction(sender, tx)
    case BlockchainActor.WatchBlockchainHeight(height) =>
      heightNotifications += HeightNotification(height, sender())
  }

  private def transactionFor(txHash: Sha256Hash): Option[MutableTransaction] =
    Option(wallet.getTransaction(txHash))

  private def broadcastTransaction(requester: ActorRef, tx: ImmutableTransaction): Unit = {
    Futures.addCallback(
      transactionBroadcaster.broadcastTransaction(tx.get),
      new FutureCallback[MutableTransaction] {
        override def onSuccess(result: MutableTransaction): Unit = {
          requester ! BlockchainActor.TransactionPublished(ImmutableTransaction(result))
        }
        override def onFailure(error: Throwable): Unit = {
          requester ! BlockchainActor.TransactionPublishingError(tx, error)
        }
      },
      context.dispatcher)
  }
}

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

  /** A message sent to the blockchain actor to request the given transaction to be
    * published (broadcast).
    */
  case class PublishTransaction(tx: ImmutableTransaction)

  /** A message sent by the blockchain actor to indicate a transaction has been successfully
    * published.
    */
  case class TransactionPublished(tx: ImmutableTransaction)

  /** A message sent by the blockchain actor indicating that something was wrong while publishing
    * the given transaction.
    */
  case class TransactionPublishingError(tx: ImmutableTransaction, error: Throwable)

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
    def blockchainActorProps(
        network: NetworkParameters,
        blockchain: AbstractBlockChain,
        transactionBroadcaster: TransactionBroadcaster): Props =
      Props(new BlockchainActor(network, blockchain, transactionBroadcaster))
  }
}
