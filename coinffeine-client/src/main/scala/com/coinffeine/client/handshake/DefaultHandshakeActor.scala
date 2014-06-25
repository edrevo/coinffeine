package com.coinffeine.client.handshake

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

    private val exchangeInfo = handshake.exchangeInfo
    private val forwarding = new MessageForwarding(
      messageGateway, exchangeInfo.counterpart.connection, exchangeInfo.broker.connection)

    def startHandshake(): Unit = {
      subscribeToMessages()
      requestRefundSignature()
      scheduleTimeouts()
      log.info("Handshake {}: Handshake started", exchangeInfo.id)
      context.become(waitForRefundSignature)
    }

    private val signCounterpartRefund: Receive = {
      case ReceiveMessage(RefundTxSignatureRequest(_, refundTransaction), _) =>
        handshake.signCounterpartRefundTransaction(refundTransaction.get) match {
          case Success(refundSignature) =>
            forwarding.forwardToCounterpart(
              RefundTxSignatureResponse(exchangeInfo.id, refundSignature))
            log.info("Handshake {}: Signing refund TX {}", exchangeInfo.id,
              refundTransaction.get.getHashAsString)
          case Failure(cause) =>
            log.warning("Handshake {}: Dropping invalid refund: {}", exchangeInfo.id, cause)
        }
    }

    private val receiveRefundSignature: Receive = {
      case ReceiveMessage(RefundTxSignatureResponse(_, refundSignature), _) =>
        handshake.validateRefundSignature(refundSignature) match {
          case Success(_) =>
            forwarding.forwardToBroker(
              ExchangeCommitment(exchangeInfo.id, handshake.commitmentTransaction))
            log.info("Handshake {}: Got a valid refund TX signature", exchangeInfo.id)
            context.become(waitForPublication(refundSignature))

          case Failure(cause) =>
            requestRefundSignature()
            log.warning("Handshake {}: Rejecting invalid refund signature: {}", exchangeInfo.id, cause)
        }

      case ResubmitRequestSignature =>
        requestRefundSignature()
        log.info("Handshake {}: Re-requesting refund signature: {}", exchangeInfo.id)

      case RequestSignatureTimeout =>
        val cause = RefundSignatureTimeoutException(exchangeInfo.id)
        forwarding.forwardToBroker(ExchangeRejection(exchangeInfo.id, cause.toString))
        finishWithResult(Failure(cause))
    }

    private def getNotifiedByBroker(refundSig: TransactionSignature): Receive = {
      case ReceiveMessage(CommitmentNotification(_, buyerTx, sellerTx), _) =>
        Set(buyerTx, sellerTx).foreach { tx =>
          blockchain ! WatchTransactionConfirmation(tx, commitmentConfirmations)
        }
        log.info("Handshake {}: The broker published {} and {}, waiting for confirmations",
          exchangeInfo.id, buyerTx, sellerTx)
        context.become(waitForConfirmations(sellerTx, buyerTx, refundSig))
    }

    private val abortOnBrokerNotification: Receive = {
      case ReceiveMessage(ExchangeAborted(_, reason), _) =>
        log.info("Handshake {}: Aborted by the broker: {}", exchangeInfo.id, reason)
        finishWithResult(Failure(HandshakeAbortedException(exchangeInfo.id, reason)))
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
          val isOwn = tx == handshake.commitmentTransaction.get.getHash
          val cause = CommitmentTransactionRejectedException(exchangeInfo.id, tx, isOwn)
          log.error("Handshake {}: {}", exchangeInfo.id, cause.getMessage)
          finishWithResult(Failure(cause))
      }
      waitForPendingConfirmations(Set(buyerTx, sellerTx))
    }

    private def subscribeToMessages(): Unit = {
      val id = exchangeInfo.id
      val broker = exchangeInfo.broker.connection
      val counterpart = exchangeInfo.counterpart.connection
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
        RefundTxSignatureRequest(exchangeInfo.id, handshake.unsignedRefundTransaction))
    }

    private def finishWithResult(result: Try[HandshakeSuccess]): Unit = {
      log.info("Handshake {}: handshake finished with result {}", exchangeInfo.id, result)
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
