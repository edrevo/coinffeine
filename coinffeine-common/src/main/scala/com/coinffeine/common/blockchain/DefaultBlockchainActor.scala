package com.coinffeine.common.blockchain

import java.util
import com.coinffeine.common.bitcoin.{ImmutableTransaction, MutableTransaction}

import scala.collection.JavaConversions._

import akka.actor.{Actor, ActorLogging, ActorRef}
import com.google.bitcoin.core._
import com.google.bitcoin.core.AbstractBlockChain.NewBlockType

class DefaultBlockchainActor(network: NetworkParameters,
                             blockchain: AbstractBlockChain) extends Actor with ActorLogging {

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
    case BlockchainActor.WatchBlockchainHeight(height) =>
      heightNotifications += HeightNotification(height, sender())
  }

  private def transactionFor(txHash: Sha256Hash): Option[MutableTransaction] =
    Option(wallet.getTransaction(txHash))
}
