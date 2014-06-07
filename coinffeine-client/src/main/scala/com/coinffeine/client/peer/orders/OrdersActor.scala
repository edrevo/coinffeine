package com.coinffeine.client.peer.orders

import akka.actor._

import com.coinffeine.client.peer.PeerActor.{CancelOrder, OpenOrder}
import com.coinffeine.common.{FiatCurrency, PeerConnection}
import com.coinffeine.common.protocol.ProtocolConstants
import com.coinffeine.common.protocol.messages.brokerage._

/** Manages open orders */
class OrdersActor(protocolConstants: ProtocolConstants) extends Actor with ActorLogging {

  override def receive: Receive = {
    case OrdersActor.Initialize(gateway, brokerAddress) =>
      new InitializedOrdersActor(gateway, brokerAddress).start()
  }

  private class InitializedOrdersActor(gateway: ActorRef, broker: PeerConnection) {

    private var delegatesByMarket = Map.empty[Market[FiatCurrency], ActorRef]

    def start(): Unit = {
      context.become(waitingForOrders)
    }

    private val waitingForOrders: Receive = {
      case message @ OpenOrder(order) =>
        getOrCreateDelegate(marketOf(order)) forward message
      case message @ CancelOrder(order) =>
        getOrCreateDelegate(marketOf(order)) forward message
    }

    private def marketOf(order: Order) = Market(currency = FiatCurrency(order.price.currency))

    private def getOrCreateDelegate(market: Market[FiatCurrency]): ActorRef =
      delegatesByMarket.getOrElse(market, createDelegate(market))

    private def createDelegate(market: Market[FiatCurrency]): ActorRef = {
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
