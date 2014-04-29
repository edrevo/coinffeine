package com.coinffeine.client.peer

import java.util.Currency

import akka.actor._

import com.coinffeine.client.peer.QuoteRequestActor.StartRequest
import com.coinffeine.common.PeerConnection
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
    case ReceiveMessage(quote: Quote, _) =>
      listener ! quote
  }

  private def subscribeToMessages(gateway: ActorRef, broker: PeerConnection): Unit = {
    gateway ! Subscribe {
      case ReceiveMessage(quote: Quote, `broker`) => true
      case _ => false
    }
  }
}

object QuoteRequestActor {

  case class StartRequest(currency: Currency, gateway: ActorRef, brokerAddress: PeerConnection)

  trait Component {
    lazy val quoteRequestProps = Props(new QuoteRequestActor())
  }
}
