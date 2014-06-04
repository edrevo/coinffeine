package com.coinffeine.client.peer

import akka.actor.{Props, ActorRef, ActorLogging, Actor}

import com.coinffeine.client.peer.OrderSubmissionActor.StartRequest
import com.coinffeine.common.PeerConnection
import com.coinffeine.common.protocol.gateway.MessageGateway.{Subscribe, ReceiveMessage, ForwardMessage}
import com.coinffeine.common.protocol.messages.brokerage.Order

/** Submits an order */
class OrderSubmissionActor extends Actor with ActorLogging {

  override def receive: Receive = {
    case StartRequest(order, gateway, brokerAddress) =>
      subscribeToMessages(gateway, brokerAddress)
      gateway ! ForwardMessage(order, brokerAddress)
      context.become(waitForResponse(gateway, brokerAddress, sender))
  }

  private def waitForResponse(
      gateway: ActorRef, broker: PeerConnection, listener: ActorRef): Receive = {
    case ReceiveMessage(order: Order, _) =>
      listener ! order
  }

  private def subscribeToMessages(gateway: ActorRef, broker: PeerConnection): Unit = {
    gateway ! Subscribe {
      case ReceiveMessage(order: Order, `broker`) => true
      case _ => false
    }
  }
}

object OrderSubmissionActor {

  case class StartRequest(order: Order, gateway: ActorRef, brokerAddress: PeerConnection)

  trait Component {
    lazy val orderSubmissionProps = Props(new OrderSubmissionActor())
  }
}
