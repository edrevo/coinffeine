package com.coinffeine.client.peer

import akka.actor._

import com.coinffeine.client.peer.QuoteRequestActor.StartRequest
import com.coinffeine.common.{FiatCurrency, PeerConnection}
import com.coinffeine.common.protocol.gateway.MessageGateway._
import com.coinffeine.common.protocol.messages.brokerage.{Quote, QuoteRequest}

/** Requests a quote */
class QuoteRequestActor extends Actor with ActorLogging {

  override def receive: Receive = {
    case StartRequest(currency, gateway, brokerAddress) =>
      subscribeToMessages(gateway, brokerAddress)
      gateway ! ForwardMessage(QuoteRequest(currency), brokerAddress)
      context.become(waitForResponse(gateway, brokerAddress, sender))
  }

  private def waitForResponse(
      gateway: ActorRef, broker: PeerConnection, listener: ActorRef): Receive = {
    case ReceiveMessage(quote: Quote[FiatCurrency], _) =>
      listener ! quote
  }

  private def subscribeToMessages(gateway: ActorRef, broker: PeerConnection): Unit = {
    gateway ! Subscribe {
      case ReceiveMessage(quote: Quote[FiatCurrency], `broker`) => true
      case _ => false
    }
  }
}

object QuoteRequestActor {

  case class StartRequest(currency: FiatCurrency, gateway: ActorRef, brokerAddress: PeerConnection)

  trait Component {
    lazy val quoteRequestProps = Props(new QuoteRequestActor())
  }
}
