package com.coinffeine.client.peer

import akka.actor._
import com.googlecode.protobuf.pro.duplex.PeerInfo

import com.coinffeine.common.PeerConnection
import com.coinffeine.common.config.ConfigComponent
import com.coinffeine.common.protocol.gateway.MessageGateway
import com.coinffeine.common.protocol.gateway.MessageGateway.{Bind, BindingError}
import com.coinffeine.common.protocol.messages.brokerage.QuoteRequest

/** Topmost actor on a peer node. It starts all the relevant actors like the peer actor and
  * the message gateway and supervise them.
  */
class PeerActor(
    address: PeerInfo,
    brokerAddress: PeerConnection,
    gatewayProps: Props,
    quoteRequestProps: Props
  ) extends Actor with ActorLogging {

  val gatewayRef = context.actorOf(gatewayProps, "gateway")

  override def preStart(): Unit = {
    gatewayRef ! Bind(address)
  }

  override def receive: Receive = {

    case BindingError(cause) =>
      log.error(cause, "Cannot start peer")
      context.stop(self)

    case QuoteRequest(currency) =>
      val request = QuoteRequestActor.StartRequest(currency, gatewayRef, brokerAddress)
      context.actorOf(quoteRequestProps) forward request
  }
}

object PeerActor {

  val HostSetting = "coinffeine.peer.host"
  val PortSetting = "coinffeine.peer.port"
  val BrokerAddressSetting = "coinffeine.broker.address"

  trait Component {
    this: QuoteRequestActor.Component with MessageGateway.Component with ConfigComponent =>

    lazy val peerProps: Props = {
      val peerInfo = new PeerInfo(config.getString(HostSetting), config.getInt(PortSetting))
      val brokerAddress = PeerConnection.parse(config.getString(BrokerAddressSetting))
      Props(new PeerActor(
        peerInfo,
        brokerAddress,
        gatewayProps = messageGatewayProps,
        quoteRequestProps = quoteRequestProps
      ))
    }
  }
}
