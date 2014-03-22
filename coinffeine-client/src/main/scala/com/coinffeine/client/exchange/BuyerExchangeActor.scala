package com.coinffeine.client.exchange

import akka.actor._

import com.coinffeine.client.{MessageForwarding, ExchangeInfo}
import com.coinffeine.client.exchange.BuyerExchangeActor.ReadyForNextOffer
import com.coinffeine.client.exchange.ExchangeActor.ExchangeSuccess
import com.coinffeine.common.protocol.{ProtocolConstants, TransactionSerialization}
import com.coinffeine.common.protocol.gateway.MessageGateway.{ReceiveMessage, Subscribe}
import com.coinffeine.common.protocol.messages.exchange.{NewOffer, OfferAccepted, PaymentProof}

class BuyerExchangeActor(
    override protected val exchangeInfo: ExchangeInfo,
    exchange: Exchange,
    override protected val messageGateway: ActorRef,
    transactionSerialization: TransactionSerialization,
    constants: ProtocolConstants,
    listeners: Seq[ActorRef]) extends Actor with ActorLogging with MessageForwarding {

  override def preStart() {
    messageGateway ! Subscribe {
      case ReceiveMessage(OfferAccepted(exchangeInfo.`id`, _), exchangeInfo.`counterpart`) => true
      case _ => false
    }
    log.info(s"Exchange ${exchangeInfo.id}: Exchange started")
    self ! ReadyForNextOffer(1)
  }

  override def receive: Receive = readyToOffer

  private val readyToOffer: Receive = {
    case ReadyForNextOffer(step) =>
      assert(step < exchangeInfo.steps)
      forwardToCounterpart(NewOffer(exchangeInfo.id, exchange.getOffer(step)))
      context.become(waitForSignedOffer(step))
  }

  private def waitForSignedOffer(step: Int): Receive = {
    case ReceiveMessage(OfferAccepted(_, signature), _) if exchange.validateSignature(step, signature) =>
      if (exchange.mustPay(step)) {
        import context.dispatcher
        forwardToCounterpart(
          exchange.pay(step).map(payment => PaymentProof(exchangeInfo.id, payment.id)))
        self ! ReadyForNextOffer(step + 1)
        context.become(readyToOffer)
      } else finishExchange()
    case ReceiveMessage(OfferAccepted(_, signature), _) =>
      log.warning("OfferAccepted message received with invaild signature. " +
        s"Step: $step, Signature: ${signature.encodeToBitcoin.toSeq} ")
  }

  private def finishExchange(): Unit = {
    log.info(s"Exchange ${exchangeInfo.id}: exchange finished with success")
    listeners.foreach { _ ! ExchangeSuccess }
    self ! PoisonPill
  }
}

object BuyerExchangeActor {
  trait Component extends ExchangeActor.Component { this: ProtocolConstants.Component =>
    override def exchangeActorProps(
        exchangeInfo: ExchangeInfo,
        exchange: Exchange,
        messageGateway: ActorRef,
        transactionSerialization: TransactionSerialization,
        resultListeners: Seq[ActorRef]): Props = Props(new BuyerExchangeActor(
      exchangeInfo, exchange, messageGateway, transactionSerialization,
      protocolConstants, resultListeners))
  }

  private case class ReadyForNextOffer(index: Int)
}
