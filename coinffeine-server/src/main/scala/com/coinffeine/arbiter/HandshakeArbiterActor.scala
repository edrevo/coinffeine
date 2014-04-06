package com.coinffeine.arbiter

import akka.actor._
import com.google.bitcoin.core.Transaction

import com.coinffeine.arbiter.HandshakeArbiterActor._
import com.coinffeine.common.PeerConnection
import com.coinffeine.common.blockchain.BlockchainActor.PublishTransaction
import com.coinffeine.common.protocol._
import com.coinffeine.common.protocol.gateway.MessageGateway._
import com.coinffeine.common.protocol.messages.PublicMessage
import com.coinffeine.common.protocol.messages.arbitration.CommitmentNotification
import com.coinffeine.common.protocol.messages.brokerage.OrderMatch
import com.coinffeine.common.protocol.messages.handshake.{EnterExchange, ExchangeAborted, ExchangeRejection}

/** A handshake arbiter is an actor able to mediate between buyer and seller to publish
  * commitment transactions at the same time.
  */
private[arbiter] class HandshakeArbiterActor(
    arbiter: CommitmentValidation,
    constants: ProtocolConstants) extends Actor with ActorLogging {

  import context.dispatcher

  private var timeout: Option[Cancellable] = None
  private var commitments = Map.empty[PeerConnection, Transaction]

  override def postStop(): Unit = timeout.foreach(_.cancel())

  override val receive: Receive = {
    case StartHandshake(orderMatch, gateway, blockchain) =>
      new HandshakeBehaviors(orderMatch, gateway, blockchain).startHandshake()
  }

  private class HandshakeBehaviors(orderMatch: OrderMatch, gateway: ActorRef, blockchain: ActorRef) {

    def startHandshake(): Unit = {
      scheduleAbortTimeout()
      subscribeToMessages()
      notifyOrderMatch()
      context.become(waitForCommitments)
    }

    private val waitForCommitments: Receive = {

      case ReceiveMessage(EnterExchange(_, tx), committer) =>
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

    private def logAlreadyCommitted(committer: PeerConnection): Unit = {
      log.warning("Exchange {}: dropping TX from {} as he has already committed one",
        orderMatch.exchangeId, committer)
    }

    private def abortOnInvalidCommitment(committer: PeerConnection, tx: Transaction): Unit = {
      log.error("Exchange {}: aborting due to invalid TX from {}: {}",
        orderMatch.exchangeId, committer, tx)
      notifyParticipants(ExchangeAborted(orderMatch.exchangeId, s"Invalid commitment from $committer"))
      self ! PoisonPill
    }

    private def acceptCommitment(committer: PeerConnection, tx: Transaction): Unit = {
      commitments += committer -> tx
      if (commitments.keySet == orderMatch.participants) {
        publishTransactions()
        notifyCommitment()
        self ! PoisonPill
      }
    }

    private def publishTransactions(): Unit =
      commitments.values.foreach(blockchain ! PublishTransaction(_))

    private def notifyOrderMatch(): Unit = notifyParticipants(orderMatch)

    private def notifyCommitment(): Unit = notifyParticipants(CommitmentNotification(
      orderMatch.exchangeId,
      commitments(orderMatch.buyer).getHash,
      commitments(orderMatch.seller).getHash
    ))

    private def notifyParticipants(notification: PublicMessage): Unit =
      orderMatch.participants.foreach { p => gateway ! ForwardMessage(notification, p) }
    private def subscribeToMessages(): Unit = {
      val id = orderMatch.exchangeId
      gateway ! Subscribe {
        case ReceiveMessage(EnterExchange(`id`, _), requester)
          if orderMatch.participants.contains(requester) => true
        case ReceiveMessage(ExchangeRejection(`id`, _), requester)
          if orderMatch.participants.contains(requester) => true
        case _ => false
      }
    }
  }

  private def scheduleAbortTimeout(): Unit = {
    timeout = Some(context.system.scheduler.scheduleOnce(
      delay = constants.commitmentAbortTimeout,
      receiver = self,
      message = AbortTimeout
    ))
  }
}

object HandshakeArbiterActor {

  case class StartHandshake(
      orderMatch: OrderMatch,
      gateway: ActorRef,
      blockchain: ActorRef
  )

  trait Component { this: ProtocolConstants.Component with CommitmentValidation.Component =>
    lazy val handshakeArbiterProps =
      Props(new HandshakeArbiterActor(commitmentValidation, protocolConstants))
  }

  private case object AbortTimeout
}
