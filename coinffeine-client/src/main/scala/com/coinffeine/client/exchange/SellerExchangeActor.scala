package com.coinffeine.client.exchange

import akka.actor.{ActorRef, ActorLogging, Actor}

import com.coinffeine.client.{ExchangeInfo, MessageForwarding}
import com.coinffeine.client.exchange.ExchangeActor.ExchangeSuccess
import com.coinffeine.common.protocol.ProtocolConstants
import com.coinffeine.common.protocol.gateway.MessageGateway.{ReceiveMessage, Subscribe}
import com.coinffeine.common.protocol.messages.exchange._
import com.coinffeine.common.protocol.messages.exchange.{OfferTransaction, PaymentProof}

/** This actor implements the seller's's side of the exchange. You can find more information about
  * the algorithm at https://github.com/Coinffeine/coinffeine/wiki/Exchange-algorithm
  */
class SellerExchangeActor(
    override protected val exchangeInfo: ExchangeInfo,
    exchange: Exchange,
    override protected val messageGateway: ActorRef,
    constants: ProtocolConstants,
    listeners: Seq[ActorRef]) extends Actor with ActorLogging with MessageForwarding  {

  override def preStart(): Unit = {
    messageGateway ! Subscribe {
      case ReceiveMessage(OfferTransaction(exchangeInfo.`id`, _), exchangeInfo.`counterpart`) => true
      case ReceiveMessage(PaymentProof(exchangeInfo.`id`, _), exchangeInfo.`counterpart`) => true
      case _ => false
    }
    log.info(s"Exchange ${exchangeInfo.id}: Exchange started")
  }

  override def receive: Receive = waitForOffer(1)

  private def waitForOffer(step: Int): Receive = {
    case ReceiveMessage(OfferTransaction(_, offer), _) if exchange.getOffer(step) == offer =>
      assert(step <= exchangeInfo.steps)
      forwardToCounterpart(OfferSignature(exchangeInfo.id, exchange.sign(offer, exchangeInfo.userKey)))
      context.become(waitForPaymentProof(step))
    case ReceiveMessage(OfferTransaction(_, offer), _) =>
      log.warning(s"Unexpected new offer received: $offer. Expected: ${exchange.getOffer(step)}")
  }

  private val waitForFinalOffer: Receive = {
    case ReceiveMessage(OfferTransaction(_, offer), _) if exchange.finalOffer == offer =>
      forwardToCounterpart(OfferSignature(exchangeInfo.id, exchange.sign(offer, exchangeInfo.userKey)))
      finishExchange()
  }

  private def waitForPaymentProof(
        step: Int,
        pendingOffers: Seq[ReceiveMessage[_]] = Seq.empty): Receive = {
    case ReceiveMessage(PaymentProof(_, paymentId), _)
      if exchange.validatePayment(step, paymentId) =>
        pendingOffers.foreach { self ! _ }
        if (step == exchangeInfo.steps) context.become(waitForFinalOffer)
        else context.become(waitForOffer(step + 1))
    case ReceiveMessage(PaymentProof(_, paymentId), _) =>
      log.warning("PaymentProof message received with invalid payment. " +
        s"Step: $step, PaymentId: $paymentId ")
    case msg @ ReceiveMessage(offer: OfferTransaction, _) =>
      context.become(waitForPaymentProof(step, pendingOffers :+ msg))
  }

  private def finishExchange(): Unit = {
    log.info(s"Exchange ${exchangeInfo.id}: exchange finished with success")
    listeners.foreach { _ ! ExchangeSuccess }
    context.stop(self)
  }
}
