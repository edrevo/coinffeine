package com.coinffeine.client.peer

import akka.actor._
import com.googlecode.protobuf.pro.duplex.PeerInfo

import com.coinffeine.client.peer.PeerActor.JoinNetwork
import com.coinffeine.common.PeerConnection
import com.coinffeine.common.protocol.gateway.MessageGateway
import com.coinffeine.common.protocol.gateway.MessageGateway.{BindingError, Bind}

/** Topmost actor on a peer node. It starts all the relevant actors like the peer actor and
  * the message gateway and supervise them.
  */
class PeerSupervisorActor(
    address: PeerInfo,
    brokerAddress: PeerConnection,
    gatewayProps: Props,
    peerProps: Props
  ) extends Actor with ActorLogging {

  val gatewayRef = context.actorOf(gatewayProps, "gateway")
  val peerRef = context.actorOf(peerProps, "peer")

  override def preStart(): Unit = {
    gatewayRef ! Bind(address)
    peerRef ! JoinNetwork(gatewayRef, brokerAddress)
  }

  override def receive: Receive = {
    case BindingError(cause) =>
      log.error(cause, "Cannot start peer")
      self ! PoisonPill
  }
}

object PeerSupervisorActor {
  trait Component {
    this: PeerActor.Component with MessageGateway.Component =>

    def peerSupervisorProps(port: Int, brokerAddress: PeerConnection): Props =
      Props(new PeerSupervisorActor(
        new PeerInfo("localhost", port),
        brokerAddress,
        gatewayProps = messageGatewayProps,
        peerProps = peerActorProps
      ))
  }
}
