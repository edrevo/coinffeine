package com.coinffeine.client.peer

import akka.actor._

import com.coinffeine.common.protocol.gateway.MessageGateway
import com.googlecode.protobuf.pro.duplex.PeerInfo

import com.coinffeine.common.PeerConnection

/** Topmost actor on a peer node. It starts all the relevant actors like the peer actor and
  * the message gateway and supervise them.
  */
class PeerSupervisorActor(
    gatewayProps: Props,
    peerProps: ActorRef => Props
  ) extends Actor with ActorLogging {

  val gatewayRef = context.actorOf(gatewayProps, "gateway")
  val peerRef = context.actorOf(peerProps(gatewayRef), "peer")

  override def receive: Receive = {
    case _ =>
  }
}

object PeerSupervisorActor {
  trait Component {
    this: PeerActor.Component with MessageGateway.Component =>

    def peerSupervisorProps(port: Int, brokerAddress: PeerConnection): Props =
      Props(new PeerSupervisorActor(
        gatewayProps = messageGatewayProps(new PeerInfo("localhost", port)),
        peerProps = _ => peerActorProps
      ))
  }
}
