package com.coinffeine.client.handshake

import scala.util.{Try, Failure, Success}

import akka.actor._
import com.google.bitcoin.core.Sha256Hash
import com.google.bitcoin.crypto.TransactionSignature

import com.coinffeine.client.MessageForwarding
import com.coinffeine.client.handshake.DefaultHandshakeActor._
import com.coinffeine.client.handshake.HandshakeActor._
import com.coinffeine.common.blockchain.BlockchainActor._
import com.coinffeine.common.protocol.ProtocolConstants
import com.coinffeine.common.protocol.gateway.MessageGateway._
import com.coinffeine.common.protocol.messages.arbitration.CommitmentNotification
import com.coinffeine.common.protocol.messages.handshake._
import com.coinffeine.common.FiatCurrency

private[handshake] class DefaultHandshakeActor[C <: FiatCurrency](handshake: Handshake[C], constants: ProtocolConstants)
  extends Actor with ActorLogging {

  import constants._
  import context.dispatcher

  private var timers = Seq.empty[Cancellable]

  override def postStop(): Unit = timers.foreach(_.cancel())

  override def receive = {
    case StartHandshake(messageGateway, blockchain, resultListeners) =>
      new InitializedHandshake(messageGateway, blockchain, resultListeners).startHandshake()
  }

  private class InitializedHandshake(
      override val messageGateway: ActorRef,
      blockchain: ActorRef,
      resultListeners: Set[ActorRef]) extends MessageForwarding  {

    def startHandshake(): Unit = {
      subscribeToMessages()
      requestRefundSignature()
      scheduleTimeouts()
      log.info("Handshake {}: Handshake started", exchangeInfo.id)
      context.become(waitForRefundSignature)
    }

    protected var exchangeInfo = handshake.exchangeInfo

    private val signCounterpartRefund: Receive = {
      case ReceiveMessage(RefundTxSignatureRequest(_, refundTransaction), _) =>
        handshake.signCounterpartRefundTransaction(refundTransaction) match {
          case Success(refundSignature) =>
            forwardToCounterpart(RefundTxSignatureResponse(exchangeInfo.id, refundSignature))
            log.info("Handshake {}: Signing refund TX {}", exchangeInfo.id,
              refundTransaction.getHashAsString)
          case Failure(cause) =>
            log.warning("Handshake {}: Dropping invalid refund: {}", exchangeInfo.id, cause)
        }
    }

    private val receiveSignedRefund: Receive = {
      case ReceiveMessage(RefundTxSignatureResponse(_, refundSignature), _) =>
        handshake.validateRefundSignature(refundSignature) match {
          case Success(_) =>
            forwardToBroker(ExchangeCommitment(exchangeInfo.id, handshake.commitmentTransaction))
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
        forwardToBroker(ExchangeRejection(exchangeInfo.id, cause.toString))
        finishWithResult(Failure(cause))
    }

    private def getNotifiedByBroker(refundSig: TransactionSignature): Receive = {
      case ReceiveMessage(CommitmentNotification(_, buyerTx, sellerTx), _) =>
        val transactions = Set(buyerTx, sellerTx)
        transactions.foreach { tx =>
          blockchain ! NotifyWhenConfirmed(tx, commitmentConfirmations)
        }
        log.info("Handshake {}: The broker published {} and {}, waiting for confirmations",
          exchangeInfo.id, buyerTx, sellerTx)
        context.become(waitForConfirmations(transactions, refundSig))
    }

    private val abortOnBrokerNotification: Receive = {
      case ReceiveMessage(ExchangeAborted(_, reason), _) =>
        log.info("Handshake {}: Aborted by the broker: {}", exchangeInfo.id, reason)
        finishWithResult(Failure(HandshakeAbortedException(exchangeInfo.id, reason)))
    }

    private val waitForRefundSignature =
      receiveSignedRefund orElse signCounterpartRefund orElse abortOnBrokerNotification

    private def waitForPublication(refundSig: TransactionSignature) =
      getNotifiedByBroker(refundSig) orElse signCounterpartRefund orElse abortOnBrokerNotification

    private def waitForConfirmations(
        pendingConfirmation: Set[Sha256Hash], refundSig: TransactionSignature): Receive = {

      case TransactionConfirmed(tx, confirmations) if confirmations >= commitmentConfirmations =>
        val stillPending = pendingConfirmation - tx
        if (!stillPending.isEmpty) {
          context.become(waitForConfirmations(stillPending, refundSig))
        } else {
          finishWithResult(Success(refundSig))
        }

      case TransactionRejected(tx) =>
        val isOwn = tx == handshake.commitmentTransaction.getHash
        val cause = CommitmentTransactionRejectedException(exchangeInfo.id, tx, isOwn)
        log.error("Handshake {}: {}", exchangeInfo.id, cause.getMessage)
        finishWithResult(Failure(cause))
    }

    private def subscribeToMessages(): Unit = {
      val id = exchangeInfo.id
      val broker = exchangeInfo.broker
      val counterpart = exchangeInfo.counterpart
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
      forwardToCounterpart(RefundTxSignatureRequest(exchangeInfo.id, handshake.refundTransaction))
    }

    private def finishWithResult(result: Try[TransactionSignature]): Unit = {
      log.info("Handshake {}: handshake finished with result {}", exchangeInfo.id, result)
      resultListeners.foreach(_ ! HandshakeResult(result))
      self ! PoisonPill
    }
  }
}

object DefaultHandshakeActor {
  trait Component extends HandshakeActor.Component { this: ProtocolConstants.Component =>
    override def handshakeActorProps[C <: FiatCurrency](handshake: Handshake[C]): Props =
      Props(new DefaultHandshakeActor(handshake, protocolConstants))
  }

  /** Internal message to remind about resubmitting refund signature requests. */
  private case object ResubmitRequestSignature
  /** Internal message that aborts the handshake. */
  private case object RequestSignatureTimeout
}
