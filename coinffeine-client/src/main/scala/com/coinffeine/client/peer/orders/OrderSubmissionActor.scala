package com.coinffeine.client.peer.orders

import akka.actor._

import com.coinffeine.common.PeerConnection
import com.coinffeine.common.protocol.ProtocolConstants
import com.coinffeine.common.protocol.gateway.MessageGateway.ForwardMessage
import com.coinffeine.common.protocol.messages.brokerage._

/** Submits and resubmits orders for a given market */
private[orders] class OrderSubmissionActor(protocolConstants: ProtocolConstants) extends Actor with ActorLogging {

  override def receive: Receive = {
    case OrderSubmissionActor.Initialize(market, gateway, brokerAddress) =>
      new InitializedOrderSubmission(market, gateway, brokerAddress).start()
  }

  private class InitializedOrderSubmission(market: Market, gateway: ActorRef, broker: PeerConnection) {

    def start(): Unit = {
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
  }

  private def orderToOrderSet(order: Order) =
    OrderSet(Market(order.price.currency)).addOrder(order.orderType, order.amount, order.price)
}

private[orders] object OrderSubmissionActor {

  case class Initialize(market: Market, gateway: ActorRef, brokerAddress: PeerConnection)

  def props(constants: ProtocolConstants) = Props(new OrderSubmissionActor(constants))
}
