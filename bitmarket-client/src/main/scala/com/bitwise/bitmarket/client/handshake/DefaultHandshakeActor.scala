package com.bitwise.bitmarket.client.handshake

import scala.util.{Try, Failure, Success}

import akka.actor._
import com.google.bitcoin.core.Sha256Hash
import com.google.bitcoin.crypto.TransactionSignature

import com.bitwise.bitmarket.client.BlockchainActor.{NotifyWhenConfirmed, TransactionConfirmed}
import com.bitwise.bitmarket.client.ProtocolConstants
import com.bitwise.bitmarket.client.handshake.HandshakeActor._
import com.bitwise.bitmarket.common.PeerConnection
import com.bitwise.bitmarket.common.protocol._
import com.bitwise.bitmarket.common.protocol.gateway.MessageGateway._

private[handshake] class DefaultHandshakeActor(
    handshake: ExchangeHandshake,
    messageGateway: ActorRef,
    blockchain: ActorRef,
    constants: ProtocolConstants,
    listeners: Seq[ActorRef]) extends Actor with ActorLogging {

  import DefaultHandshakeActor._
  import constants._
  import context.dispatcher

  private var timers = Seq.empty[Cancellable]

  override def preStart() {
    messageGateway ! Subscribe {
      case ReceiveMessage(RefundTxSignatureRequest(handshake.`id`, _), handshake.`counterpart`) => true
      case ReceiveMessage(RefundTxSignatureResponse(handshake.`id`, _), handshake.`counterpart`) => true
      case ReceiveMessage(CommitmentNotification(handshake.`id`, _, _), handshake.`broker`) => true
      case _ => false
    }
    requestRefundSignature()
    timers = Seq(
      context.system.scheduler.schedule(resubmitRefundSignatureTimeout, resubmitRefundSignatureTimeout) {
        self ! ResubmitRequestSignature
      },
      context.system.scheduler.scheduleOnce(refundSignatureAbortTimeout) {
        self ! RequestSignatureTimeout
      }
    )
    log.info("Handshake {}: Handshake started", handshake.id)
  }

  override def postStop() {
    timers.foreach(_.cancel())
  }

  override def receive = waitForRefundSignature

  private val signCounterpartRefund: Receive = {
    case ReceiveMessage(RefundTxSignatureRequest(handshake.id, refundTransaction), _) =>
      handshake.signCounterpartRefundTransaction(refundTransaction) match {
        case Success(refundSignature) =>
          forwardToCounterpart(RefundTxSignatureResponse(handshake.id, refundSignature))
          log.info("Handshake {}: Signing refund TX {}", handshake.id,
            refundTransaction.getHashAsString)
        case Failure(cause) =>
          log.warning("Handshake {}: Dropping invalid refund: {}", handshake.id, cause)
      }
  }

  private val receiveSignedRefund: Receive = {
    case ReceiveMessage(RefundTxSignatureResponse(handshake.id, refundSignature), _) =>
      handshake.validateRefundSignature(refundSignature) match {
        case Success(_) =>
          forwardToBroker(EnterExchange(handshake.id, handshake.commitmentTransaction))
          log.info("Handshake {}: Got a valid refund TX signature", handshake.id)
          context.become(waitForPublication(refundSignature))

        case Failure(cause) =>
          requestRefundSignature()
          log.warning("Handshake {}: Rejecting invalid refund signature: {}", handshake.id, cause)
      }

    case ResubmitRequestSignature =>
      requestRefundSignature()
      log.info("Handshake {}: Re-requesting refund signature: {}", handshake.id)

    case RequestSignatureTimeout =>
      val cause = RefundSignatureTimeoutException(handshake.id)
      forwardToBroker(RejectExchange(handshake.id, cause.toString))
      notifyResult(Failure(cause))
      self ! PoisonPill
  }

  private def getNotifiedByBroker(refundSig: TransactionSignature): Receive = {
    case ReceiveMessage(CommitmentNotification(handshake.id, buyerTx, sellerTx), _) =>
      val transactions = Set(buyerTx, sellerTx)
      transactions.foreach { tx =>
        blockchain ! NotifyWhenConfirmed(tx, commitmentConfirmations)
      }
      log.info("Handshake {}: The broker published {} and {}, waiting for confirmations",
        handshake.id, buyerTx, sellerTx)
      context.become(waitForConfirmations(transactions, refundSig))
  }

  private val waitForRefundSignature = receiveSignedRefund orElse signCounterpartRefund

  private def waitForPublication(refundSig: TransactionSignature) =
    getNotifiedByBroker(refundSig) orElse signCounterpartRefund

  private def waitForConfirmations(
      pendingConfirmation: Set[Sha256Hash], refundSig: TransactionSignature): Receive = {
    case TransactionConfirmed(tx, confirmations) if confirmations >= commitmentConfirmations =>
      val stillPending = pendingConfirmation - tx
      if (!stillPending.isEmpty) {
        context.become(waitForConfirmations(stillPending, refundSig))
      } else {
        notifyResult(Success(refundSig))
        self ! PoisonPill
      }
  }

  private def requestRefundSignature() {
    forwardToCounterpart(RefundTxSignatureRequest(handshake.id, handshake.refundTransaction))
  }

  private def notifyResult(result: Try[TransactionSignature]) {
    log.info("Handshake {}: handshake finished with result {}", handshake.id, result)
    listeners.foreach(_ ! HandshakeResult(result))
  }

  private def forwardToCounterpart(message: Any) { forwardMessage(message, handshake.counterpart) }

  private def forwardToBroker(message: Any) { forwardMessage(message, handshake.broker) }

  private def forwardMessage(message: Any, address: PeerConnection) {
    messageGateway ! ForwardMessage(message, address)
  }
}

object DefaultHandshakeActor {
  trait Component extends HandshakeActor.Component { this: ProtocolConstants.Component =>
    override def handshakeActorProps(
        exchangeHandshake: ExchangeHandshake,
        messageGateway: ActorRef,
        blockchain: ActorRef,
        listeners: Seq[ActorRef]): Props = Props(new DefaultHandshakeActor(
      exchangeHandshake, messageGateway, blockchain, protocolConstants, listeners
    ))
  }

  /** Internal message to remind about resubmitting refund signature requests. */
  private case object ResubmitRequestSignature
  /** Internal message that aborts the handshake. */
  private case object RequestSignatureTimeout
}
