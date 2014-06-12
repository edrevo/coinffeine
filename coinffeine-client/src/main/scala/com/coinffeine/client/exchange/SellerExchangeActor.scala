package com.coinffeine.client.exchange

import scala.util.{Try, Failure}

import akka.actor._

import com.coinffeine.client.{ExchangeInfo, MessageForwarding}
import com.coinffeine.client.exchange.ExchangeActor.{StartExchange, ExchangeSuccess}
import com.coinffeine.client.exchange.SellerExchangeActor.PaymentValidationResult
import com.coinffeine.common.protocol.ProtocolConstants
import com.coinffeine.common.protocol.gateway.MessageGateway.{ReceiveMessage, Subscribe}
import com.coinffeine.common.protocol.messages.exchange._
import com.coinffeine.common.FiatCurrency

/** This actor implements the seller's's side of the exchange. You can find more information about
  * the algorithm at https://github.com/Coinffeine/coinffeine/wiki/Exchange-algorithm
  */
class SellerExchangeActor[C <: FiatCurrency](exchange: Exchange[C] with SellerUser[C], constants: ProtocolConstants)
  extends Actor with ActorLogging with Stash {

  override def receive: Receive = {
    case StartExchange(messageGateway, resultListeners) =>
      new InitializedSellerExchange(messageGateway, resultListeners)
  }

  private class InitializedSellerExchange(messageGateway: ActorRef, listeners: Set[ActorRef]) {

    private val exchangeInfo = exchange.exchangeInfo
    private val forwarding = new MessageForwarding(
      messageGateway, exchangeInfo.counterpart, exchangeInfo.broker)

    messageGateway ! Subscribe {
      case ReceiveMessage(PaymentProof(exchangeInfo.`id`, _), exchangeInfo.`counterpart`) => true
      case _ => false
    }
    log.info(s"Exchange ${exchangeInfo.id}: Exchange started")
    forwarding.forwardToCounterpart(StepSignatures(
      exchangeInfo.id,
      exchange.signStep(1)))
    context.become(waitForPaymentProof(1))

    private def waitForPaymentProof(step: Int): Receive = {
      case ReceiveMessage(PaymentProof(_, paymentId), _) =>
        import context.dispatcher
        exchange.validatePayment(step, paymentId).onComplete { tryResult =>
            self ! PaymentValidationResult(tryResult)
        }
        context.become(waitForPaymentValidation(paymentId, step))
    }

    private def waitForPaymentValidation(paymentId: String, step: Int): Receive = {
      case PaymentValidationResult(Failure(cause)) =>
        unstashAll()
        log.warning(s"Invalid payment proof received in step $step: $paymentId. Reason: $cause")
        context.become(waitForPaymentProof(step))
      case PaymentValidationResult(_) =>
        if (step == exchangeInfo.steps) finishExchange()
        else transitionToNextStep(step)
      case _ => stash()
    }

    private def transitionToNextStep(currentStep: Int): Unit = {
      unstashAll()
      forwarding.forwardToCounterpart(StepSignatures(
        exchangeInfo.id,
        exchange.signStep(currentStep)))
      context.become(waitForPaymentProof(currentStep + 1))
    }

    private def finishExchange(): Unit = {
      log.info(s"Exchange ${exchangeInfo.id}: exchange finished with success")
      forwarding.forwardToCounterpart(StepSignatures(
        exchangeInfo.id,
        exchange.finalSignature))
      listeners.foreach { _ ! ExchangeSuccess }
      context.stop(self)
    }
  }
}

object SellerExchangeActor {
  private case class PaymentValidationResult(result: Try[Unit])

  trait Component { this: ProtocolConstants.Component =>
    def exchangeActorProps[C <: FiatCurrency](exchange: Exchange[C] with SellerUser[C]): Props =
      Props(new SellerExchangeActor(exchange, protocolConstants))
  }
}
