package com.coinffeine.client.exchange

import scala.util.{Failure, Success}

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
    case StartExchange(messageGateway, resultListeners) =>
      new InitializedSellerExchange(messageGateway, resultListeners)
  }

  private class InitializedSellerExchange(
      override protected val messageGateway: ActorRef,
      listeners: Set[ActorRef]) extends MessageForwarding {

    override protected val exchangeInfo: ExchangeInfo = exchange.exchangeInfo

    messageGateway ! Subscribe {
      case ReceiveMessage(PaymentProof(exchangeInfo.`id`, _), exchangeInfo.`counterpart`) => true
      case _ => false
    }
    log.info(s"Exchange ${exchangeInfo.id}: Exchange started")
    forwardToCounterpart(StepSignature(
      exchangeInfo.id,
      exchange.signStep(1)))
    context.become(waitForPaymentProof(1))

    private def waitForPaymentProof(step: Int): Receive = {
      case ReceiveMessage(PaymentProof(_, paymentId), _) =>
        exchange.validatePayment(step, paymentId) match {
          case Success(_) =>
            if (step == exchangeInfo.steps) finishExchange()
            else {
              forwardToCounterpart(StepSignature(
                exchangeInfo.id,
                exchange.signStep(step)))
              context.become(waitForPaymentProof(step + 1))
            }
          case Failure(cause) =>
            log.warning(s"Invalid payment proof received in step $step: $paymentId. Reason: $cause")
        }
    }

    private def finishExchange(): Unit = {
      log.info(s"Exchange ${exchangeInfo.id}: exchange finished with success")
      forwardToCounterpart(StepSignature(
        exchangeInfo.id,
        exchange.finalSignature))
      listeners.foreach { _ ! ExchangeSuccess }
      context.stop(self)
    }
  }
}
