package com.coinffeine.client.peer

import java.util.Currency

import akka.actor._

import com.coinffeine.client.peer.PeerActor.JoinNetwork
import com.coinffeine.common.PeerConnection
import com.coinffeine.common.protocol.gateway.MessageGateway._
import com.coinffeine.common.protocol.messages.brokerage.{Quote, QuoteRequest}

/** A peer that is able to take part in multiple exchanges. */
class PeerActor() extends Actor with ActorLogging {

  override def receive: Receive = {
    case JoinNetwork(messageGateway, brokerAddress) =>
      new PeerOnNetwork(messageGateway, brokerAddress).join()
  }

  private class PeerOnNetwork(gateway: ActorRef, broker: PeerConnection) {

    def join(): Unit = {
      subscribeToMessages(broker)
      context.become(receive)
    }

    private var quoteRequesters = Map.empty[Currency, ActorRef]

    private val receive: Receive = {
      case request @ QuoteRequest(currency) =>
        quoteRequesters += currency -> sender
        gateway ! ForwardMessage(request, broker)

      case ReceiveMessage(quote: Quote, _) =>
        quoteRequesters.get(quote.currency).foreach { ref =>
          ref ! quote
        }
        quoteRequesters -= quote.currency
    }

    private def subscribeToMessages(broker: PeerConnection): Unit = gateway ! Subscribe {
      case ReceiveMessage(quote: Quote, `broker`) => true
      case _ => false
    }
  }
}

object PeerActor {

  /** Order the peer to join the network */
  case class JoinNetwork(messageGateway: ActorRef, brokerAddress: PeerConnection)

  trait Component {
    lazy val peerActorProps = Props(new PeerActor())
  }
}
