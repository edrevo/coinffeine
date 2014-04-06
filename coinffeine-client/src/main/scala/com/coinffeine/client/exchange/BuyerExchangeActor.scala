package com.coinffeine.client.exchange

import akka.actor._

import com.coinffeine.client.{MessageForwarding, ExchangeInfo}
import com.coinffeine.client.exchange.BuyerExchangeActor.{ReadyForFinalOffer, ReadyForNextOffer}
import com.coinffeine.client.exchange.ExchangeActor.ExchangeSuccess
import com.coinffeine.common.protocol.ProtocolConstants
import com.coinffeine.common.protocol.gateway.MessageGateway.{ReceiveMessage, Subscribe}
import com.coinffeine.common.protocol.messages.exchange.{OfferTransaction, OfferSignature, PaymentProof}

/** This actor implements the buyer's side of the exchange. You can find more information about
  * the algorithm at https://github.com/Coinffeine/coinffeine/wiki/Exchange-algorithm
  */
class BuyerExchangeActor(
    override protected val exchangeInfo: ExchangeInfo,
    exchange: Exchange,
    override protected val messageGateway: ActorRef,
    constants: ProtocolConstants,
    listeners: Seq[ActorRef]) extends Actor with ActorLogging with MessageForwarding {

  override def preStart() {
    messageGateway ! Subscribe {
      case ReceiveMessage(OfferSignature(exchangeInfo.`id`, _), exchangeInfo.`counterpart`) => true
      case _ => false
    }
    log.info(s"Exchange ${exchangeInfo.id}: Exchange started")
    self ! ReadyForNextOffer(1)
  }

  override def receive: Receive = readyToOffer

  private val waitForSignedFinalOffer: Receive = {
    case ReceiveMessage(OfferSignature(_, signature), _)
      if exchange.validateFinalSignature(signature) =>
      log.info(s"Exchange ${exchangeInfo.id}: exchange finished with success")
      // TODO: Publish transaction to blockchain
      listeners.foreach { _ ! ExchangeSuccess }
      context.stop(self)
  }

  private val readyToOffer: Receive = {
    case ReadyForNextOffer(step) =>
      assert(step <= exchangeInfo.steps)
      forwardToCounterpart(OfferTransaction(exchangeInfo.id, exchange.getOffer(step)))
      context.become(waitForSignedOffer(step))
    case ReadyForFinalOffer =>
      forwardToCounterpart(OfferTransaction(exchangeInfo.id, exchange.finalOffer))
      context.become(waitForSignedFinalOffer)
  }

  private def waitForSignedOffer(step: Int): Receive = {
    case ReceiveMessage(OfferSignature(_, signature), _)
      if exchange.validateSignature(step, signature) =>
        import context.dispatcher
        forwardToCounterpart(
          exchange.pay(step).map(payment => PaymentProof(exchangeInfo.id, payment.id)))
        if (step == exchangeInfo.steps)
          self ! ReadyForFinalOffer
        else
          self ! ReadyForNextOffer(step + 1)
        context.become(readyToOffer)
    case ReceiveMessage(OfferSignature(_, signature), _) =>
      log.warning("OfferAccepted message received with invalid signature. " +
        s"Step: $step, Signature: ${signature.encodeToBitcoin.toSeq} ")
  }
}

object BuyerExchangeActor {
  trait Component extends ExchangeActor.Component { this: ProtocolConstants.Component =>
    override def exchangeActorProps(
        exchangeInfo: ExchangeInfo,
        exchange: Exchange,
        messageGateway: ActorRef,
        resultListeners: Seq[ActorRef]): Props = Props(new BuyerExchangeActor(
      exchangeInfo, exchange, messageGateway, protocolConstants, resultListeners))
  }

  private case class ReadyForNextOffer(index: Int)
  private case object ReadyForFinalOffer

}
