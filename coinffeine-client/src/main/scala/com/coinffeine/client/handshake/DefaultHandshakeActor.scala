package com.coinffeine.client.handshake

import scala.util.{Try, Failure, Success}

import akka.actor._
import com.google.bitcoin.core.Sha256Hash
import com.google.bitcoin.crypto.TransactionSignature

import com.coinffeine.client.MessageForwarding
import com.coinffeine.client.handshake.DefaultHandshakeActor._
import com.coinffeine.client.handshake.HandshakeActor._
import com.coinffeine.common.blockchain.BlockchainActor._
import com.coinffeine.common.protocol.{ProtocolConstants, TransactionSerialization}
import com.coinffeine.common.protocol.gateway.MessageGateway._
import com.coinffeine.common.protocol.messages.arbitration.CommitmentNotification
import com.coinffeine.common.protocol.messages.handshake._

private[handshake] class DefaultHandshakeActor(
    handshake: Handshake,
    override protected val messageGateway: ActorRef,
    blockchain: ActorRef,
    transactionSerialization: TransactionSerialization,
    constants: ProtocolConstants,
    listeners: Seq[ActorRef]) extends Actor with ActorLogging with MessageForwarding {

  import constants._
  import context.dispatcher

  private var timers = Seq.empty[Cancellable]
  override protected val exchangeInfo = handshake.exchangeInfo

  override def preStart() {
    messageGateway ! Subscribe {
      case ReceiveMessage(RefundTxSignatureRequest(exchangeInfo.`id`, _), exchangeInfo.`counterpart`) => true
      case ReceiveMessage(RefundTxSignatureResponse(exchangeInfo.`id`, _), exchangeInfo.`counterpart`) => true
      case ReceiveMessage(CommitmentNotification(exchangeInfo.`id`, _, _), exchangeInfo.`broker`) => true
      case ReceiveMessage(ExchangeAborted(exchangeInfo.`id`, _), exchangeInfo.`broker`) => true
      case _ => false
    }
    requestRefundSignature()
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
    log.info("Handshake {}: Handshake started", exchangeInfo.id)
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
          forwardToBroker(EnterExchange(exchangeInfo.id,
            transactionSerialization.serializeTransaction(handshake.commitmentTransaction)))
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

  private def requestRefundSignature() {
    forwardToCounterpart(RefundTxSignatureRequest(
      exchangeInfo.id, transactionSerialization.serializeTransaction(handshake.refundTransaction)))
  }

  private def finishWithResult(result: Try[TransactionSignature]) {
    log.info("Handshake {}: handshake finished with result {}", exchangeInfo.id, result)
    listeners.foreach(_ ! HandshakeResult(result))
    self ! PoisonPill
  }
}

object DefaultHandshakeActor {
  trait Component extends HandshakeActor.Component { this: ProtocolConstants.Component =>
    override def handshakeActorProps(
        handshake: Handshake,
        messageGateway: ActorRef,
        blockchain: ActorRef,
        transactionSerialization: TransactionSerialization,
        listeners: Seq[ActorRef]): Props = Props(new DefaultHandshakeActor(
          handshake, messageGateway, blockchain,
          transactionSerialization, protocolConstants, listeners))
  }

  /** Internal message to remind about resubmitting refund signature requests. */
  private case object ResubmitRequestSignature
  /** Internal message that aborts the handshake. */
  private case object RequestSignatureTimeout
}
