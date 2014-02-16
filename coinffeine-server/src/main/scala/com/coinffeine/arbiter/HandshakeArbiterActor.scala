package com.coinffeine.arbiter

import akka.actor._
import com.google.bitcoin.core.Transaction

import com.coinffeine.arbiter.HandshakeArbiterActor._
import com.coinffeine.common.PeerConnection
import com.coinffeine.common.blockchain.BlockchainActor.PublishTransaction
import com.coinffeine.common.protocol._
import com.coinffeine.common.protocol.gateway.MessageGateway._

/** A handshake arbiter is an actor able to mediate between buyer and seller to publish
  * commitment transactions at the same time.
  */
private[arbiter] class HandshakeArbiterActor(
    arbiter: CommitmentValidation,
    gateway: ActorRef,
    blockchain: ActorRef,
    transactionSerialization: TransactionSerialization,
    constants: ProtocolConstants) extends Actor with ActorLogging {

  import context.dispatcher

  private var timeout: Option[Cancellable] = None
  private var commitments = Map.empty[PeerConnection, Transaction]
  private var orderMatch: OrderMatch = null

  override def postStop() { timeout.foreach(_.cancel()) }

  override def receive: Receive = {
    case initializationMessage: OrderMatch =>
      orderMatch = initializationMessage
      subscribeToMessages()
      scheduleAbortTimeout()
      context.become(waitForCommitments)
  }

  private def subscribeToMessages() {
    val id = orderMatch.exchangeId
    gateway ! Subscribe {
      case ReceiveMessage(EnterExchange(`id`, _), requester)
        if orderMatch.participants.contains(requester) => true
      case ReceiveMessage(ExchangeRejection(`id`, _), requester)
        if orderMatch.participants.contains(requester) => true
      case _ => false
    }
  }

  private def scheduleAbortTimeout() {
    timeout = Some(context.system.scheduler.scheduleOnce(
      delay = constants.commitmentAbortTimeout,
      receiver = self,
      message = AbortTimeout
    ))
  }

  private val waitForCommitments: Receive = {

    case ReceiveMessage(EnterExchange(_, txBytes), committer) =>
      val tx = transactionSerialization.deserializeTransaction(txBytes) // TODO: what if deserialization fails?
      if (commitments.contains(committer)) logAlreadyCommitted(committer)
      else if (!arbiter.isValidCommitment(committer, tx)) abortOnInvalidCommitment(committer, tx)
      else acceptCommitment(committer, tx)

    case ReceiveMessage(ExchangeRejection(_, reason), requester) =>
      val counterpart = (orderMatch.participants - requester).head
      val notification = ExchangeAborted(orderMatch.exchangeId, s"Rejected by counterpart: $reason")
      gateway ! ForwardMessage(notification, counterpart)
      self ! PoisonPill

    case AbortTimeout =>
      log.error("Exchange {}: aborting on timeout", orderMatch.exchangeId)
      notifyParticipants(ExchangeAborted(orderMatch.exchangeId, "Timeout waiting for commitments"))
      self ! PoisonPill
  }

  private def logAlreadyCommitted(committer: PeerConnection) {
    log.warning("Exchange {}: dropping TX from {} as he has already committed one",
      orderMatch.exchangeId, committer)
  }

  private def abortOnInvalidCommitment(committer: PeerConnection, tx: Transaction) {
    log.error("Exchange {}: aborting due to invalid TX from {}: {}",
      orderMatch.exchangeId, committer, tx)
    notifyParticipants(ExchangeAborted(orderMatch.exchangeId, s"Invalid commitment from $committer"))
    self ! PoisonPill
  }

  private def acceptCommitment(committer: PeerConnection, tx: Transaction) {
    commitments += committer -> tx
    if (commitments.keySet == orderMatch.participants) {
      publishTransactions()
      notifyCommitment()
      self ! PoisonPill
    }
  }

  private def publishTransactions() {
    commitments.values.foreach(blockchain ! PublishTransaction(_))
  }

  private def notifyCommitment() {
    notifyParticipants(CommitmentNotification(
      orderMatch.exchangeId,
      commitments(orderMatch.buyer).getHash,
      commitments(orderMatch.seller).getHash
    ))
  }

  private def notifyParticipants[T: MessageSend](notification: T) {
    orderMatch.participants.foreach { p => gateway ! ForwardMessage(notification, p) }
  }
}

object HandshakeArbiterActor {
  trait Component { this: ProtocolConstants.Component with TransactionSerialization.Component =>
    def handshakeArbiterActor(
        arbiter: CommitmentValidation,
        gateway: ActorRef,
        blockchain: ActorRef) = Props(new HandshakeArbiterActor(
      arbiter, gateway, blockchain, transactionSerialization, protocolConstants))
  }

  private case object AbortTimeout
}
