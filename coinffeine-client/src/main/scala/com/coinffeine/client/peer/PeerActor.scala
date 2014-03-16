package com.coinffeine.client.peer

import java.util.Currency

import akka.actor._

import com.coinffeine.common.PeerConnection
import com.coinffeine.common.protocol.gateway.MessageGateway._
import com.coinffeine.common.protocol.messages.brokerage.{Quote, QuoteRequest}

/** A peer that is able to take part in multiple exchanges. */
class PeerActor(gateway: ActorRef, broker: PeerConnection) extends Actor with ActorLogging {

  private var quoteRequesters = Map.empty[Currency, ActorRef]

  override def preStart(): Unit = gateway ! Subscribe {
    case ReceiveMessage(quote: Quote, `broker`) => true
    case _ => false
  }

  override def receive: Receive = {

    case request @ QuoteRequest(currency) =>
      quoteRequesters += currency -> sender
      gateway ! ForwardMessage(request, broker)

    case ReceiveMessage(quote: Quote, _) =>
      quoteRequesters.get(quote.currency).foreach { ref =>
        ref ! quote
      }
      quoteRequesters -= quote.currency
  }
}

object PeerActor {
  trait Component {
    def peerActorProps(gateway: ActorRef, broker: PeerConnection): Props =
      Props(new PeerActor(gateway, broker))
  }
}
