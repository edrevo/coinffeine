package com.coinffeine.client.exchange

import akka.actor.{Stash, ActorRef, ActorLogging, Actor}

import com.coinffeine.client.{ExchangeInfo, MessageForwarding}
import com.coinffeine.client.exchange.ExchangeActor.{StartExchange, ExchangeSuccess}
import com.coinffeine.common.protocol.ProtocolConstants
import com.coinffeine.common.protocol.gateway.MessageGateway.{ReceiveMessage, Subscribe}
import com.coinffeine.common.protocol.messages.exchange._

/** This actor implements the seller's's side of the exchange. You can find more information about
  * the algorithm at https://github.com/Coinffeine/coinffeine/wiki/Exchange-algorithm
  */
class SellerExchangeActor(exchange: Exchange, constants: ProtocolConstants)
  extends Actor with ActorLogging with Stash {

  override def receive: Receive = {
    case StartExchange(exchangeInfo, messageGateway, resultListeners) =>
      new InitializedSellerExchange(exchangeInfo, messageGateway, resultListeners)
  }

  private class InitializedSellerExchange(
      override protected val exchangeInfo: ExchangeInfo,
      override protected val messageGateway: ActorRef,
      listeners: Set[ActorRef]) extends MessageForwarding {

    messageGateway ! Subscribe {
      case ReceiveMessage(PaymentProof(exchangeInfo.`id`, _), exchangeInfo.`counterpart`) => true
      case _ => false
    }
    log.info(s"Exchange ${exchangeInfo.id}: Exchange started")
    forwardToCounterpart(StepSignature(
      exchangeInfo.id,
      exchange.getStepSignature(1, exchangeInfo.userKey)))
    context.become(waitForPaymentProof(1))

    private def waitForPaymentProof(step: Int): Receive = {
      case ReceiveMessage(PaymentProof(_, paymentId), _)
        if exchange.validatePayment(step, paymentId) =>
          if (step == exchangeInfo.steps) finishExchange()
          else {
            forwardToCounterpart(StepSignature(
              exchangeInfo.id,
              exchange.getStepSignature(step, exchangeInfo.userKey)))
            context.become(waitForPaymentProof(step + 1))
          }
      case ReceiveMessage(PaymentProof(_, paymentId), _) =>
        log.warning("PaymentProof message received with invalid payment. " +
          s"Step: $step, PaymentId: $paymentId ")
    }

    private def finishExchange(): Unit = {
      log.info(s"Exchange ${exchangeInfo.id}: exchange finished with success")
      forwardToCounterpart(StepSignature(
        exchangeInfo.id,
        exchange.sign(exchange.finalOffer, exchangeInfo.userKey)))
      listeners.foreach { _ ! ExchangeSuccess }
      context.stop(self)
    }
  }
}
