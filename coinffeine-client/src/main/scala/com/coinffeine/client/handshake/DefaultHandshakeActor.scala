package com.coinffeine.client.handshake

import com.coinffeine.common.exchange.Handshake.InvalidRefundTransaction

import scala.util.{Failure, Success, Try}

import akka.actor._

import com.coinffeine.client.MessageForwarding
import com.coinffeine.client.handshake.DefaultHandshakeActor._
import com.coinffeine.client.handshake.HandshakeActor._
import com.coinffeine.common.FiatCurrency
import com.coinffeine.common.bitcoin.{Hash, TransactionSignature}
import com.coinffeine.common.blockchain.BlockchainActor._
import com.coinffeine.common.protocol.ProtocolConstants
import com.coinffeine.common.protocol.gateway.MessageGateway._
import com.coinffeine.common.protocol.messages.arbitration.CommitmentNotification
import com.coinffeine.common.protocol.messages.handshake._

private[handshake] class DefaultHandshakeActor[C <: FiatCurrency]
  extends Actor with ActorLogging {

  private var timers = Seq.empty[Cancellable]

  override def postStop(): Unit = timers.foreach(_.cancel())

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
      case ReceiveMessage(RefundTxSignatureRequest(_, refundTransaction), _) =>
        try {
          val refundSignature = handshake.signHerRefund(refundTransaction)
          forwarding.forwardToCounterpart(RefundTxSignatureResponse(exchange.id, refundSignature))
          log.info("Handshake {}: Signing refund TX {}", exchange.id,
            refundTransaction.get.getHashAsString)
        } catch {
          case cause: InvalidRefundTransaction =>
            log.warning("Handshake {}: Dropping invalid refund: {}", exchange.id, cause)
        }
    }

    private val receiveRefundSignature: Receive = {
      case ReceiveMessage(RefundTxSignatureResponse(_, refundSignature), _) =>
        handshake.validateRefundSignature(refundSignature) match {
          case Success(_) =>
            forwarding.forwardToBroker(
              ExchangeCommitment(exchange.id, handshake.myDeposit))
            log.info("Handshake {}: Got a valid refund TX signature", exchange.id)
            context.become(waitForPublication(refundSignature))

          case Failure(cause) =>
            requestRefundSignature()
            log.warning("Handshake {}: Rejecting invalid refund signature: {}", exchange.id, cause)
        }

      case ResubmitRequestSignature =>
        requestRefundSignature()
        log.info("Handshake {}: Re-requesting refund signature: {}", exchange.id)

      case RequestSignatureTimeout =>
        val cause = RefundSignatureTimeoutException(exchange.id)
        forwarding.forwardToBroker(ExchangeRejection(exchange.id, cause.toString))
        finishWithResult(Failure(cause))
    }

    private def getNotifiedByBroker(refundSig: TransactionSignature): Receive = {
      case ReceiveMessage(CommitmentNotification(_, buyerTx, sellerTx), _) =>
        Set(buyerTx, sellerTx).foreach { tx =>
          blockchain ! WatchTransactionConfirmation(tx, commitmentConfirmations)
        }
        log.info("Handshake {}: The broker published {} and {}, waiting for confirmations",
          exchange.id, buyerTx, sellerTx)
        context.become(waitForConfirmations(sellerTx, buyerTx, refundSig))
    }

    private val abortOnBrokerNotification: Receive = {
      case ReceiveMessage(ExchangeAborted(_, reason), _) =>
        log.info("Handshake {}: Aborted by the broker: {}", exchange.id, reason)
        finishWithResult(Failure(HandshakeAbortedException(exchange.id, reason)))
    }

    private val waitForRefundSignature =
      receiveRefundSignature orElse signCounterpartRefund orElse abortOnBrokerNotification

    private def waitForPublication(refundSig: TransactionSignature) =
      getNotifiedByBroker(refundSig) orElse signCounterpartRefund orElse abortOnBrokerNotification

    private def waitForConfirmations(
        sellerTx: Hash, buyerTx: Hash, refundSig: TransactionSignature): Receive = {
      def waitForPendingConfirmations(pendingConfirmation: Set[Hash]): Receive = {
        case TransactionConfirmed(tx, confirmations) if confirmations >= commitmentConfirmations =>
          val stillPending = pendingConfirmation - tx
          if (stillPending.nonEmpty) {
            context.become(waitForPendingConfirmations(stillPending))
          } else {
            finishWithResult(Success(HandshakeSuccess(sellerTx, buyerTx, refundSig)))
          }

        case TransactionRejected(tx) =>
          val isOwn = tx == handshake.myDeposit.get.getHash
          val cause = CommitmentTransactionRejectedException(exchange.id, tx, isOwn)
          log.error("Handshake {}: {}", exchange.id, cause.getMessage)
          finishWithResult(Failure(cause))
      }
      waitForPendingConfirmations(Set(buyerTx, sellerTx))
    }

    private def subscribeToMessages(): Unit = {
      val id = exchange.id
      val broker = exchange.broker.connection
      val counterpart = role.her(exchange).connection
      messageGateway ! Subscribe {
        case ReceiveMessage(RefundTxSignatureRequest(`id`, _), `counterpart`) => true
        case ReceiveMessage(RefundTxSignatureResponse(`id`, _), `counterpart`) => true
        case ReceiveMessage(CommitmentNotification(`id`, _, _), `broker`) => true
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
      forwarding.forwardToCounterpart(
        RefundTxSignatureRequest(exchange.id, handshake.myUnsignedRefund))
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

object DefaultHandshakeActor {
  trait Component extends HandshakeActor.Component { this: ProtocolConstants.Component =>
    override def handshakeActorProps[C <: FiatCurrency]: Props =
      Props[DefaultHandshakeActor[C]]
  }

  /** Internal message to remind about resubmitting refund signature requests. */
  private case object ResubmitRequestSignature
  /** Internal message that aborts the handshake. */
  private case object RequestSignatureTimeout
}
