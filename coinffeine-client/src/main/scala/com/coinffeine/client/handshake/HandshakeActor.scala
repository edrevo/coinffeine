package com.coinffeine.client.handshake

import scala.util.{Try, Success, Failure}

import akka.actor._

import com.coinffeine.client.MessageForwarding
import com.coinffeine.client.handshake.HandshakeActor._
import com.coinffeine.common.FiatCurrency
import com.coinffeine.common.bitcoin.{Hash, ImmutableTransaction}
import com.coinffeine.common.blockchain.BlockchainActor.{TransactionRejected, TransactionConfirmed, WatchTransactionConfirmation}
import com.coinffeine.common.exchange._
import com.coinffeine.common.exchange.Handshake.{InvalidRefundSignature, InvalidRefundTransaction}
import com.coinffeine.common.protocol.ProtocolConstants
import com.coinffeine.common.protocol.gateway.MessageGateway.{Subscribe, ReceiveMessage}
import com.coinffeine.common.protocol.messages.arbitration.CommitmentNotification
import com.coinffeine.common.protocol.messages.handshake._

private[handshake] class HandshakeActor[C <: FiatCurrency]
  extends Actor with ActorLogging {

  private var timers = Seq.empty[Cancellable]

  override def postStop(): Unit = {
    timers.foreach(_.cancel())
  }

  override def receive = {
    case init: StartHandshake[C] => new InitializedHandshake(init).startHandshake()
  }

  private class InitializedHandshake(init: StartHandshake[C]) {
    import context.dispatcher
    import init._
    import init.constants._

    private val forwarding = new MessageForwarding(messageGateway, exchange, role)

    def startHandshake(): Unit = {
      subscribeToMessages()
      requestRefundSignature()
      scheduleTimeouts()
      log.info("Handshake {}: Handshake started", exchange.id)
      context.become(waitForRefundSignature)
    }

    private val signCounterpartRefund: Receive = {
      case ReceiveMessage(PeerHandshake(_, refundTransaction, _), _) =>
        try {
          val refundSignature = handshake.signHerRefund(refundTransaction)
          forwarding.forwardToCounterpart(PeerHandshakeAccepted(exchange.id, refundSignature))
          log.info("Handshake {}: Signing refund TX {}", exchange.id,
            refundTransaction.get.getHashAsString)
        } catch {
          case cause: InvalidRefundTransaction =>
            log.warning("Handshake {}: Dropping invalid refund: {}", exchange.id, cause)
        }
    }

    private val receiveRefundSignature: Receive = {
      case ReceiveMessage(PeerHandshakeAccepted(_, herSignature), _) =>
        try {
          val myRefund = handshake.signMyRefund(herSignature)
          forwarding.forwardToBroker(ExchangeCommitment(exchange.id, handshake.myDeposit))
          log.info("Handshake {}: Got a valid refund TX signature", exchange.id)
          context.become(waitForPublication(myRefund))
        } catch {
          case cause: InvalidRefundSignature =>
            requestRefundSignature()
            log.warning("Handshake {}: Rejecting invalid refund signature: {}", exchange.id, cause)
        }

      case ResubmitRequestSignature =>
        requestRefundSignature()
        log.info("Handshake {}: Re-requesting refund signature", exchange.id)

      case RequestSignatureTimeout =>
        val cause = RefundSignatureTimeoutException(exchange.id)
        forwarding.forwardToBroker(ExchangeRejection(exchange.id, cause.toString))
        finishWithResult(Failure(cause))
    }

    private def getNotifiedByBroker(refund: ImmutableTransaction): Receive = {
      case ReceiveMessage(CommitmentNotification(_, bothCommitments), _) =>
        bothCommitments.toSeq.foreach { tx =>
          blockchain ! WatchTransactionConfirmation(tx, commitmentConfirmations)
        }
        log.info("Handshake {}: The broker published {}, waiting for confirmations",
          exchange.id, bothCommitments)
        context.become(waitForConfirmations(bothCommitments, refund))
    }

    private val abortOnBrokerNotification: Receive = {
      case ReceiveMessage(ExchangeAborted(_, reason), _) =>
        log.info("Handshake {}: Aborted by the broker: {}", exchange.id, reason)
        finishWithResult(Failure(HandshakeAbortedException(exchange.id, reason)))
    }

    private val waitForRefundSignature =
      receiveRefundSignature orElse signCounterpartRefund orElse abortOnBrokerNotification

    private def waitForPublication(refund: ImmutableTransaction) =
      getNotifiedByBroker(refund) orElse signCounterpartRefund orElse abortOnBrokerNotification

    private def waitForConfirmations(
                                      bothCommitments: Both[Hash], refund: ImmutableTransaction): Receive = {
      def waitForPendingConfirmations(pendingConfirmation: Set[Hash]): Receive = {
        case TransactionConfirmed(tx, confirmations) if confirmations >= commitmentConfirmations =>
          val stillPending = pendingConfirmation - tx
          if (stillPending.nonEmpty) {
            context.become(waitForPendingConfirmations(stillPending))
          } else {
            finishWithResult(Success(HandshakeSuccess(bothCommitments, refund)))
          }

        case TransactionRejected(tx) =>
          val isOwn = tx == handshake.myDeposit.get.getHash
          val cause = CommitmentTransactionRejectedException(exchange.id, tx, isOwn)
          log.error("Handshake {}: {}", exchange.id, cause.getMessage)
          finishWithResult(Failure(cause))
      }
      waitForPendingConfirmations(bothCommitments.toSet)
    }

    private def subscribeToMessages(): Unit = {
      val id = exchange.id
      val broker = exchange.broker.connection
      val counterpart = exchange.connections(role.counterpart)
      messageGateway ! Subscribe {
        case ReceiveMessage(PeerHandshake(`id`, _, _), `counterpart`) => true
        case ReceiveMessage(PeerHandshakeAccepted(`id`, _), `counterpart`) => true
        case ReceiveMessage(CommitmentNotification(`id`, _), `broker`) => true
        case ReceiveMessage(ExchangeAborted(`id`, _), `broker`) => true
        case _ => false
      }
    }

    private def scheduleTimeouts(): Unit = {
      timers = Seq(
        context.system.scheduler.schedule(
          initialDelay = resubmitRefundSignatureTimeout,
          interval = resubmitRefundSignatureTimeout,
          receiver = self,
          message = ResubmitRequestSignature
        ),
        context.system.scheduler.scheduleOnce(
          delay = refundSignatureAbortTimeout,
          receiver = self,
          message = RequestSignatureTimeout
        )
      )
    }

    private def requestRefundSignature(): Unit = {
      forwarding.forwardToCounterpart(PeerHandshake(
        exchange.id, handshake.myUnsignedRefund, exchange.participants(role).paymentProcessorAccount))
    }

    private def finishWithResult(result: Try[HandshakeSuccess]): Unit = {
      log.info("Handshake {}: handshake finished with result {}", exchange.id, result)
      resultListeners.foreach(_ ! result.recover {
        case e => HandshakeFailure(e)
      }.get)
      self ! PoisonPill
    }
  }
}

/** A handshake actor is in charge of entering into a value exchange by getting a refundSignature
  * transaction signed and relying on the broker to publish the commitment TX.
  */
object HandshakeActor {

  /** Sent to the actor to start the handshake
    *
    * @constructor
    * @param exchange         Exchange to start the handshake for
    * @param role             Which role to take
    * @param handshake        Class that contains the logic to perform the handshake
    * @param constants        Protocol constants
    * @param messageGateway   Communications gateway
    * @param blockchain       Actor to ask for TX confirmations for
    * @param resultListeners  Actors to be notified of the handshake result
    */
  case class StartHandshake[C <: FiatCurrency](
      exchange: OngoingExchange[C], // TODO: reduce visibility to just Exchange[C]
      role: Role,
      handshake: Handshake,
      constants: ProtocolConstants,
      messageGateway: ActorRef,
      blockchain: ActorRef,
      resultListeners: Set[ActorRef])

  /** Sent to the handshake listeners to notify success with a refundSignature transaction or
    * failure with an exception.
    */
  case class HandshakeSuccess(bothCommitments: Both[Hash], refundTransaction: ImmutableTransaction)

  case class HandshakeFailure(e: Throwable)

  case class RefundSignatureTimeoutException(exchangeId: Exchange.Id) extends RuntimeException(
    s"Timeout waiting for a valid signature of the refund transaction of handshake $exchangeId")

  case class CommitmentTransactionRejectedException(
       exchangeId: Exchange.Id, rejectedTx: Hash, isOwn: Boolean) extends RuntimeException(
    s"Commitment transaction $rejectedTx (${if (isOwn) "ours" else "counterpart"}) was rejected"
  )

  case class HandshakeAbortedException(exchangeId: Exchange.Id, reason: String)
    extends RuntimeException(s"Handshake $exchangeId aborted externally: $reason")

  /** Internal message to remind about resubmitting refund signature requests. */
  private case object ResubmitRequestSignature
  /** Internal message that aborts the handshake. */
  private case object RequestSignatureTimeout

  trait Component {
    /** Create the properties of a handshake actor.
      *
      * @return                 Actor properties
      */
    def handshakeActorProps[C <: FiatCurrency]: Props = Props[HandshakeActor[C]]
  }
}
