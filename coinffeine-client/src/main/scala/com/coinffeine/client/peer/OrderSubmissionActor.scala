package com.coinffeine.client.peer

import akka.actor.{Actor, ActorLogging, ActorRef, Props}

import com.coinffeine.client.peer.OrderSubmissionActor.StartRequest
import com.coinffeine.common.PeerConnection
import com.coinffeine.common.protocol.gateway.MessageGateway.{ForwardMessage, ReceiveMessage, Subscribe}
import com.coinffeine.common.protocol.messages.brokerage._

/** Submits an order */
class OrderSubmissionActor extends Actor with ActorLogging {

  override def receive: Receive = {
    case StartRequest(order, gateway, brokerAddress) =>
      val orderSet = orderToOrderSet(order)
      subscribeToMessages(gateway, brokerAddress)
      gateway ! ForwardMessage(orderSet, brokerAddress)
      context.become(waitForResponse(gateway, brokerAddress, sender))
  }

  private def waitForResponse(
      gateway: ActorRef, broker: PeerConnection, listener: ActorRef): Receive = {
    case ReceiveMessage(orderSet: OrderSet, _) =>
      listener ! orderSet
  }

  private def subscribeToMessages(gateway: ActorRef, broker: PeerConnection): Unit = {
    gateway ! Subscribe {
      case ReceiveMessage(order: Order, `broker`) => true
      case _ => false
    }
  }

  private def orderToOrderSet(order: Order) = {
    val entry = OrderSet.Entry(order.amount, order.price)
    OrderSet(
      Market(currency = order.price.currency),
      bids = if (order.orderType == Bid) Seq(entry) else Seq.empty,
      asks = if (order.orderType == Ask) Seq(entry) else Seq.empty
    )
  }
}

object OrderSubmissionActor {

  case class StartRequest(order: Order, gateway: ActorRef, brokerAddress: PeerConnection)

  trait Component {
    lazy val orderSubmissionProps = Props(new OrderSubmissionActor())
  }
}
