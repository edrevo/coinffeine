package com.bitwise.bitmarket.client.handshake

import scala.util.{Try, Failure, Success}

import akka.actor._
import com.google.bitcoin.core.{Transaction, Sha256Hash}
import com.google.bitcoin.crypto.TransactionSignature

import com.bitwise.bitmarket.client.ProtocolConstants
import com.bitwise.bitmarket.client.handshake.DefaultHandshakeActor._
import com.bitwise.bitmarket.client.handshake.HandshakeActor._
import com.bitwise.bitmarket.common.PeerConnection
import com.bitwise.bitmarket.common.blockchain.BlockchainActor._
import com.bitwise.bitmarket.common.protocol._
import com.bitwise.bitmarket.common.protocol.gateway.MessageGateway._

private[handshake] class DefaultHandshakeActor(
    handshake: ExchangeHandshake,
    messageGateway: ActorRef,
    blockchain: ActorRef,
    transactionSerialization: TransactionSerialization,
    constants: ProtocolConstants,
    listeners: Seq[ActorRef]) extends Actor with ActorLogging {

  import constants._
  import context.dispatcher

  private var timers = Seq.empty[Cancellable]
  private val exchange = handshake.exchange

  override def preStart() {
    messageGateway ! Subscribe {
      case ReceiveMessage(RefundTxSignatureRequest(exchange.`id`, _), exchange.`counterpart`) => true
      case ReceiveMessage(RefundTxSignatureResponse(exchange.`id`, _), exchange.`counterpart`) => true
      case ReceiveMessage(CommitmentNotification(exchange.`id`, _, _), exchange.`broker`) => true
      case ReceiveMessage(ExchangeAborted(exchange.`id`, _), exchange.`broker`) => true
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
    log.info("Handshake {}: Handshake started", exchange.id)
  }

  override def postStop() {
    timers.foreach(_.cancel())
  }

  override def receive = waitForRefundSignature

  private val signCounterpartRefund: Receive = {
    case ReceiveMessage(RefundTxSignatureRequest(_, refundTransactionBytes), _) =>
      val refundTransaction = transactionSerialization.deserializeTransaction(refundTransactionBytes)
      handshake.signCounterpartRefundTransaction(refundTransaction) match {
        case Success(refundSignature) =>
          forwardToCounterpart(RefundTxSignatureResponse(exchange.id, refundSignature))
          log.info("Handshake {}: Signing refund TX {}", exchange.id,
            refundTransaction.getHashAsString)
        case Failure(cause) =>
          log.warning("Handshake {}: Dropping invalid refund: {}", exchange.id, cause)
      }
  }

  private val receiveSignedRefund: Receive = {
    case ReceiveMessage(RefundTxSignatureResponse(_, refundSignature), _) =>
      handshake.validateRefundSignature(refundSignature) match {
        case Success(_) =>
          forwardToBroker(EnterExchange(exchange.id,
            transactionSerialization.serializeTransaction(handshake.commitmentTransaction)))
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
      forwardToBroker(ExchangeRejection(exchange.id, cause.toString))
      finishWithResult(Failure(cause))
  }

  private def getNotifiedByBroker(refundSig: TransactionSignature): Receive = {
    case ReceiveMessage(CommitmentNotification(_, buyerTx, sellerTx), _) =>
      val transactions = Set(buyerTx, sellerTx)
      transactions.foreach { tx =>
        blockchain ! NotifyWhenConfirmed(tx, commitmentConfirmations)
      }
      log.info("Handshake {}: The broker published {} and {}, waiting for confirmations",
        exchange.id, buyerTx, sellerTx)
      context.become(waitForConfirmations(transactions, refundSig))
  }

  private val abortOnBrokerNotification: Receive = {
    case ReceiveMessage(ExchangeAborted(_, reason), _) =>
      log.info("Handshake {}: Aborted by the broker: {}", exchange.id, reason)
      finishWithResult(Failure(HandshakeAbortedException(exchange.id, reason)))
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
      val cause = CommitmentTransactionRejectedException(exchange.id, tx, isOwn)
      log.error("Handshake {}: {}", exchange.id, cause.getMessage)
      finishWithResult(Failure(cause))
  }

  private def requestRefundSignature() {
    forwardToCounterpart(RefundTxSignatureRequest(
      exchange.id, transactionSerialization.serializeTransaction(handshake.refundTransaction)))
  }

  private def finishWithResult(result: Try[TransactionSignature]) {
    log.info("Handshake {}: handshake finished with result {}", exchange.id, result)
    listeners.foreach(_ ! HandshakeResult(result))
    self ! PoisonPill
  }

  private def forwardToCounterpart[T : MessageSend](message: T) {
    forwardMessage(message, exchange.counterpart)
  }

  private def forwardToBroker[T : MessageSend](message: T) {
    forwardMessage(message, exchange.broker)
  }

  private def forwardMessage[T : MessageSend](message: T, address: PeerConnection) {
    messageGateway ! ForwardMessage(message, address)
  }
}

object DefaultHandshakeActor {
  trait Component extends HandshakeActor.Component { this: ProtocolConstants.Component =>
    override def handshakeActorProps(
        exchangeHandshake: ExchangeHandshake,
        messageGateway: ActorRef,
        blockchain: ActorRef,
        transactionSerialization: TransactionSerialization,
        listeners: Seq[ActorRef]): Props = Props(new DefaultHandshakeActor(
          exchangeHandshake, messageGateway, blockchain,
          transactionSerialization, protocolConstants, listeners))
  }

  /** Internal message to remind about resubmitting refund signature requests. */
  private case object ResubmitRequestSignature
  /** Internal message that aborts the handshake. */
  private case object RequestSignatureTimeout
}
