package com.coinffeine.client.peer

import akka.actor._

import com.coinffeine.common.PeerConnection
import com.coinffeine.common.protocol.ProtocolConstants
import com.coinffeine.common.protocol.gateway.MessageGateway.{ForwardMessage, ReceiveMessage, Subscribe}
import com.coinffeine.common.protocol.messages.brokerage._

/** Submits an order */
class OrderSubmissionActor(protocolConstants: ProtocolConstants) extends Actor with ActorLogging {

  override def receive: Receive = {
    case OrderSubmissionActor.Initialize(gateway, brokerAddress) =>
      new InitializedOrderSubmission(gateway, brokerAddress).start()
  }

  private class InitializedOrderSubmission(gateway: ActorRef, broker: PeerConnection) {

    def start(): Unit = {
      subscribeToMessages()
      context.become(waitingForOrders)
    }

    private val waitingForOrders: Receive = {
      case order: Order =>
        val orderSet = orderToOrderSet(order)
        forwardOrders(orderSet)
        context.become(keepingOpenOrders(orderSet))
    }

    private def keepingOpenOrders(orderSet: OrderSet): Receive = {
      case order: Order =>
        val mergedOrderSet = orderSet.addOrder(order.orderType, order.amount, order.price)
        forwardOrders(mergedOrderSet)
        context.become(keepingOpenOrders(mergedOrderSet))

      case ReceiveTimeout =>
        forwardOrders(orderSet)
    }

    private def forwardOrders(orderSet: OrderSet): Unit = {
      gateway ! ForwardMessage(orderSet, broker)
      context.setReceiveTimeout(protocolConstants.orderResubmitInterval)
    }

    private def subscribeToMessages(): Unit = {
      gateway ! Subscribe {
        case ReceiveMessage(_: Order, `broker`) => true
        case _ => false
      }
    }
  }

  private def orderToOrderSet(order: Order) =
    OrderSet(Market(order.price.currency)).addOrder(order.orderType, order.amount, order.price)
}

object OrderSubmissionActor {

  case class Initialize(gateway: ActorRef, brokerAddress: PeerConnection)

  trait Component { this: ProtocolConstants.Component =>
    lazy val orderSubmissionProps = Props(new OrderSubmissionActor(protocolConstants))
  }
}
