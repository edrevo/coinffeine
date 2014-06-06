package com.coinffeine.client.peer.orders

import akka.actor._

import com.coinffeine.common.PeerConnection
import com.coinffeine.common.protocol.ProtocolConstants
import com.coinffeine.common.protocol.messages.brokerage._

/** Manages open orders */
class OrdersActor(protocolConstants: ProtocolConstants) extends Actor with ActorLogging {

  override def receive: Receive = {
    case OrdersActor.Initialize(gateway, brokerAddress) =>
      new InitializedOrdersActor(gateway, brokerAddress).start()
  }

  private class InitializedOrdersActor(gateway: ActorRef, broker: PeerConnection) {

    private var delegatesByMarket = Map.empty[Market, ActorRef]

    def start(): Unit = {
      context.become(waitingForOrders)
    }

    private val waitingForOrders: Receive = {
      case order: Order =>
        getOrCreateDelegate(marketOf(order)) forward order
    }

    private def marketOf(order: Order) = Market(currency = order.price.currency)

    private def getOrCreateDelegate(market: Market): ActorRef =
      delegatesByMarket.getOrElse(market, createDelegate(market))

    private def createDelegate(market: Market): ActorRef = {
      log.info(s"Start submitting to $market")
      val newDelegate = context.actorOf(OrderSubmissionActor.props(protocolConstants))
      newDelegate ! OrderSubmissionActor.Initialize(market, gateway, broker)
      delegatesByMarket += market -> newDelegate
      newDelegate
    }
  }
}

object OrdersActor {

  case class Initialize(gateway: ActorRef, brokerAddress: PeerConnection)

  trait Component { this: ProtocolConstants.Component =>
    lazy val ordersActorProps = Props(new OrdersActor(protocolConstants))
  }
}
