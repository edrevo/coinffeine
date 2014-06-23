package com.coinffeine.client.micropayment

import scala.util.{Failure, Success, Try}

import akka.actor._

import com.coinffeine.client.MessageForwarding
import com.coinffeine.client.exchange.BuyerUser
import com.coinffeine.client.micropayment.MicroPaymentChannelActor._
import com.coinffeine.common.FiatCurrency
import com.coinffeine.common.bitcoin.{MutableTransaction, TransactionSignature}
import com.coinffeine.common.protocol.ProtocolConstants
import com.coinffeine.common.protocol.gateway.MessageGateway.{ReceiveMessage, Subscribe}
import com.coinffeine.common.protocol.messages.exchange.{PaymentProof, StepSignatures}

/** This actor implements the buyer's side of the exchange. You can find more information about
  * the algorithm at https://github.com/Coinffeine/coinffeine/wiki/Exchange-algorithm
  */
class BuyerMicroPaymentChannelActor[C <: FiatCurrency]
  extends Actor with ActorLogging with StepTimeout  {

  private var stepTimers = Seq.empty[Cancellable]

  override def postStop(): Unit = stepTimers.foreach(_.cancel())

  override def receive: Receive = {
    case init: StartMicroPaymentChannel[C, BuyerUser[C]] =>
      new InitializedBuyer(init).startExchange()
  }

  private class InitializedBuyer(init: StartMicroPaymentChannel[C, BuyerUser[C]]) {
    import init._
    import init.constants.exchangeSignatureTimeout

    private val exchangeInfo = exchange.exchangeInfo
    private val forwarding = new MessageForwarding(
      messageGateway, exchangeInfo.counterpart, exchangeInfo.broker)
    private val finalStep = exchangeInfo.steps + 1

    private var lastSignedOffer: Option[MutableTransaction] = None

    def startExchange(): Unit = {
      subscribeToMessages()
      context.become(waitForNextStepSignature(1))
      log.info(s"Exchange ${exchangeInfo.id}: Exchange started")
    }

    private def subscribeToMessages(): Unit = messageGateway ! Subscribe {
      case ReceiveMessage(StepSignatures(exchangeInfo.`id`, _, _, _), exchangeInfo.`counterpart`) => true
      case _ => false
    }

    private def handleTimeout(step: Int): Receive= {
      case StepSignatureTimeout =>
        val errorMsg = s"Timed out waiting for the seller to provide the signature for step $step" +
          s" (out of ${exchangeInfo.steps}})"
        log.warning(errorMsg)
        finishWith(ExchangeFailure(
          TimeoutException(errorMsg),
          lastSignedOffer))
    }

    private def withStepTimeout(step: Int)(receive: Receive): Receive = {
      scheduleStepTimeouts(exchangeSignatureTimeout)
      receive.andThen(_ => cancelTimeout()).orElse(handleTimeout(step))
    }

    private def waitForValidSignature(
        step: Int,
        signatureCondition: (TransactionSignature, TransactionSignature) => Try[Unit])(
        body: (TransactionSignature, TransactionSignature) => Unit): Receive = {
      case ReceiveMessage(StepSignatures(_, `step`, signature0, signature1), _) =>
        signatureCondition(signature0, signature1) match {
          case Success(_) =>
            body(signature0, signature1)
          case Failure(cause) =>
            log.warning(
              s"Received invalid signature for step $step: ($signature0, $signature1). Reason: $cause")
            finishWith(ExchangeFailure(
              InvalidStepSignature(step, signature0, signature1, cause), lastSignedOffer))
        }
    }

    private val waitForFinalSignature: Receive = withStepTimeout(finalStep) {
      waitForValidSignature(finalStep, exchange.validateSellersFinalSignature) {
        (signature0, signature1) =>
          log.info(s"Exchange ${exchangeInfo.id}: exchange finished with success")
          // TODO: Publish transaction to blockchain
          finishWith(ExchangeSuccess)
      }
    }

    private def finishWith(result: Any): Unit = {
      resultListeners.foreach { _ ! result }
      context.stop(self)
    }

    private def waitForNextStepSignature(step: Int): Receive = withStepTimeout(step) {
      waitForValidSignature(step, exchange.validateSellersSignature(step, _, _)) {
        (signature0, signature1) =>
          import context.dispatcher
          lastSignedOffer = Some(exchange.getSignedOffer(step, (signature0, signature1)))
          forwarding.forwardToCounterpart(
            exchange.pay(step).map(payment => PaymentProof(exchangeInfo.id, payment.id)))
          context.become(nextWait(step))
      }
    }

    private def nextWait(step: Int): Receive =
      if (step == exchangeInfo.steps) waitForFinalSignature
      else waitForNextStepSignature(step + 1)
  }
}

object BuyerMicroPaymentChannelActor {
  trait Component { this: ProtocolConstants.Component =>
    def micropaymentChannelActorProps[C <: FiatCurrency]: Props =
      Props[BuyerMicroPaymentChannelActor[C]]
  }
}
