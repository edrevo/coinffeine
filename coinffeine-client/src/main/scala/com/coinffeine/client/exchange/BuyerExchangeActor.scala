package com.coinffeine.client.exchange

import scala.util.{Failure, Success}

import akka.actor._

import com.coinffeine.client.MessageForwarding
import com.coinffeine.client.exchange.ExchangeActor._
import com.coinffeine.common.FiatCurrency
import com.coinffeine.common.protocol.ProtocolConstants
import com.coinffeine.common.protocol.gateway.MessageGateway.{ReceiveMessage, Subscribe}
import com.coinffeine.common.protocol.messages.exchange.{PaymentProof, StepSignatures}

/** This actor implements the buyer's side of the exchange. You can find more information about
  * the algorithm at https://github.com/Coinffeine/coinffeine/wiki/Exchange-algorithm
  */
class BuyerExchangeActor[C <: FiatCurrency] extends Actor with ActorLogging  {

  override def receive: Receive = {
    case init: StartExchange[C, BuyerUser[C]] =>
      new InitializedBuyerExchange(init).startExchange()
  }

  private class InitializedBuyerExchange(init: StartExchange[C, BuyerUser[C]]) {
    import init._

    private val exchangeInfo = exchange.exchangeInfo
    private val forwarding = new MessageForwarding(
      messageGateway, exchangeInfo.counterpart, exchangeInfo.broker)

    def startExchange(): Unit = {
      subscribeToMessages()
      context.become(waitForNextStepSignature(1))
      log.info(s"Exchange ${exchangeInfo.id}: Exchange started")
    }

    private def subscribeToMessages(): Unit = messageGateway ! Subscribe {
      case ReceiveMessage(StepSignatures(exchangeInfo.`id`, _, _, _), exchangeInfo.`counterpart`) => true
      case _ => false
    }

    private val waitForFinalSignature: Receive = {
      val finalStep = exchangeInfo.steps + 1

      {
        case ReceiveMessage(StepSignatures(_, `finalStep`, signature0, signature1), _) =>
          exchange.validateSellersFinalSignature(signature0, signature1) match {
            case Success(_) =>
              log.info(s"Exchange ${exchangeInfo.id}: exchange finished with success")
              // TODO: Publish transaction to blockchain
              finishWith(ExchangeSuccess)
            case Failure(cause) =>
              log.warning(s"Received invalid final signature: ($signature0, $signature1). Reason: $cause")
          }
      }
    }

    private def finishWith(result: Any): Unit = {
      resultListeners.foreach { _ ! result }
      context.stop(self)
    }

    private def waitForNextStepSignature(step: Int): Receive = {
      case ReceiveMessage(StepSignatures(_, `step`, signature0, signature1), _) =>
        exchange.validateSellersSignature(step, signature0, signature1) match {
          case Success(_) =>
            import context.dispatcher
            forwarding.forwardToCounterpart(
              exchange.pay(step).map(payment => PaymentProof (exchangeInfo.id, payment.id)))
            context.become(nextWait(step))
          case Failure(cause) =>
            log.warning(s"Received invalid signature ($signature0, $signature1) in step $step. Reason: $cause")
        }
    }

    private def nextWait(step: Int): Receive =
      if (step == exchangeInfo.steps) waitForFinalSignature
      else waitForNextStepSignature(step + 1)
  }
}

object BuyerExchangeActor {
  trait Component { this: ProtocolConstants.Component =>
    def exchangeActorProps[C <: FiatCurrency]: Props = Props[BuyerExchangeActor[C]]
  }
}
