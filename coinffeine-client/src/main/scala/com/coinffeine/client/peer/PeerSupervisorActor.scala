package com.coinffeine.client.peer

import akka.actor._

import com.coinffeine.common.protocol.gateway.MessageGateway
import com.coinffeine.common.system.SupervisorComponent
import com.googlecode.protobuf.pro.duplex.PeerInfo

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
  trait Component extends SupervisorComponent {
    this: PeerActor.Component with MessageGateway.Component =>

    override def supervisorProps(args: Array[String]): Props = {
      val cli = PeerCommandLine.fromArgList(args)
      Props(new PeerSupervisorActor(
        gatewayProps = messageGatewayProps(new PeerInfo("localhost", cli.port)),
        peerProps = gateway => peerActorProps(gateway, cli.brokerAddress)
      ))
    }
  }
}
