package com.coinffeine.client.exchange

import akka.actor._

import com.coinffeine.client.{ExchangeInfo, MessageForwarding}
import com.coinffeine.client.exchange.ExchangeActor.{ExchangeSuccess, StartExchange}
import com.coinffeine.common.protocol.ProtocolConstants
import com.coinffeine.common.protocol.gateway.MessageGateway.{ReceiveMessage, Subscribe}
import com.coinffeine.common.protocol.messages.exchange.{StepSignature, PaymentProof}

/** This actor implements the buyer's side of the exchange. You can find more information about
  * the algorithm at https://github.com/Coinffeine/coinffeine/wiki/Exchange-algorithm
  */
class BuyerExchangeActor(exchange: Exchange, constants: ProtocolConstants)
  extends Actor with ActorLogging  {

  override def receive: Receive = {
    case StartExchange(exchangeInfo, messageGateway, resultListeners) =>
      new InitializedBuyerExchange(exchangeInfo, messageGateway, resultListeners).startExchange()
  }

  private class InitializedBuyerExchange(
      override val exchangeInfo: ExchangeInfo,
      override val messageGateway: ActorRef,
      listeners: Set[ActorRef]) extends MessageForwarding {

    def startExchange(): Unit = {
      subscribeToMessages()
      context.become(waitForNextStepSignature(1))
      log.info(s"Exchange ${exchangeInfo.id}: Exchange started")
    }

    private def subscribeToMessages(): Unit = messageGateway ! Subscribe {
      case ReceiveMessage(StepSignature(exchangeInfo.`id`, _), exchangeInfo.`counterpart`) => true
      case _ => false
    }

    private val waitForFinalSignature: Receive = {
      case ReceiveMessage(StepSignature(_, signature), _)
        if exchange.validateFinalSignature(signature) =>
          log.info(s"Exchange ${exchangeInfo.id}: exchange finished with success")
          // TODO: Publish transaction to blockchain
          listeners.foreach { _ ! ExchangeSuccess }
          context.stop(self)
    }

    private def waitForNextStepSignature(step: Int): Receive = {
      case ReceiveMessage(StepSignature(_, signature), _)
        if exchange.validateSignature(step, signature) =>
        import context.dispatcher
        forwardToCounterpart(
          exchange.pay(step).map(payment => PaymentProof(exchangeInfo.id, payment.id)))
        if (step == exchangeInfo.steps) {
          context.become(waitForFinalSignature)
        } else {
          context.become(waitForNextStepSignature(step + 1))
        }
      case ReceiveMessage(StepSignature(_, signature), _) =>
        log.warning("StepSignature message received with invalid signature. " +
          s"Step: $step, Signature: ${signature.encodeToBitcoin.toSeq} ")
    }
  }
}

object BuyerExchangeActor {
  trait Component extends ExchangeActor.Component { this: ProtocolConstants.Component =>
    override def exchangeActorProps(exchange: Exchange): Props =
      Props(new BuyerExchangeActor(exchange, protocolConstants))
  }
}
