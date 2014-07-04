package com.coinffeine.client.peer.orders

import akka.actor._

import com.coinffeine.client.peer.PeerActor.{CancelOrder, OpenOrder}
import com.coinffeine.common.{Order, FiatCurrency, PeerConnection}
import com.coinffeine.common.protocol.ProtocolConstants
import com.coinffeine.common.protocol.gateway.MessageGateway.ForwardMessage
import com.coinffeine.common.protocol.messages.brokerage._

/** Submits and resubmits orders for a given market */
private[orders] class OrderSubmissionActor(protocolConstants: ProtocolConstants)
  extends Actor with ActorLogging {

  override def receive: Receive = {
    case OrderSubmissionActor.Initialize(market, gateway, brokerAddress) =>
      new InitializedOrderSubmission(market, gateway, brokerAddress).start()
  }

  private class InitializedOrderSubmission(
      market: Market[FiatCurrency], gateway: ActorRef, broker: PeerConnection) {

    def start(): Unit = {
      context.become(waitingForOrders)
    }

    private val waitingForOrders: Receive = handleOpenOrders(OrderSet.empty(market))

    private def keepingOpenOrders(orderSet: OrderSet[FiatCurrency]): Receive =
      handleOpenOrders(orderSet).orElse{
        case CancelOrder(order) =>
          val reducedOrderSet = orderSet.cancelOrder(
            order.orderType, order.amount, order.price)
          forwardOrders(reducedOrderSet)
          context.become(
            if (reducedOrderSet.isEmpty) waitingForOrders
            else keepingOpenOrders(reducedOrderSet)
          )

        case ReceiveTimeout =>
          forwardOrders(orderSet)
      }

    private def handleOpenOrders(orderSet: OrderSet[FiatCurrency]): Receive = {
      case OpenOrder(order) =>
        val mergedOrderSet = orderSet.addOrder(
          order.orderType, order.amount, order.price)
        forwardOrders(mergedOrderSet)
        context.become(keepingOpenOrders(mergedOrderSet))
    }

    private def forwardOrders(orderSet: OrderSet[FiatCurrency]): Unit = {
      gateway ! ForwardMessage(orderSet, broker)
      context.setReceiveTimeout(protocolConstants.orderResubmitInterval)
    }
  }

  private def orderToOrderSet(order: Order) =
    OrderSet.empty(Market(order.price.currency)).addOrder(
      order.orderType, order.amount, order.price)
}

private[orders] object OrderSubmissionActor {

  case class Initialize(market: Market[FiatCurrency], gateway: ActorRef, brokerAddress: PeerConnection)

  def props(constants: ProtocolConstants) = Props(new OrderSubmissionActor(constants))
}
