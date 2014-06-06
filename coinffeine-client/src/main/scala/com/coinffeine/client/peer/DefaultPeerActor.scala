package com.coinffeine.client.peer

import scala.concurrent.duration._

import akka.actor._
import akka.pattern._
import akka.util.Timeout
import com.googlecode.protobuf.pro.duplex.PeerInfo

import com.coinffeine.client.peer.orders.OrdersActor
import com.coinffeine.common.PeerConnection
import com.coinffeine.common.config.ConfigComponent
import com.coinffeine.common.protocol.gateway.MessageGateway
import com.coinffeine.common.protocol.gateway.MessageGateway.{Bind, BindingError, BoundTo}
import com.coinffeine.common.protocol.messages.brokerage.{Order, QuoteRequest}

/** Implementation of the topmost actor on a peer node. It starts all the relevant actors like
  * the peer actor and the message gateway and supervise them.
  */
class DefaultPeerActor(address: PeerInfo,
                       brokerAddress: PeerConnection,
                       gatewayProps: Props,
                       quoteRequestProps: Props,
                       ordersActorProps: Props) extends Actor with ActorLogging {

  import context.dispatcher

  val gatewayRef = context.actorOf(gatewayProps, "gateway")
  val ordersActorRef = {
    val ref = context.actorOf(ordersActorProps, "orders")
    ref ! OrdersActor.Initialize(gatewayRef, brokerAddress)
    ref
  }

  override def receive: Receive = {

    case PeerActor.Connect =>
      implicit val timeout = DefaultPeerActor.ConnectionTimeout
      (gatewayRef ? Bind(address)).map {
        case BoundTo(_) => PeerActor.Connected
        case BindingError(cause) => PeerActor.ConnectionFailed(cause)
      }.pipeTo(sender)

    case BindingError(cause) =>
      log.error(cause, "Cannot start peer")
      context.stop(self)

    case QuoteRequest(currency) =>
      val request = QuoteRequestActor.StartRequest(currency, gatewayRef, brokerAddress)
      context.actorOf(quoteRequestProps) forward request

    case order: Order =>
      ordersActorRef ! order
  }
}

object DefaultPeerActor {

  val HostSetting = "coinffeine.peer.host"
  val PortSetting = "coinffeine.peer.port"
  val BrokerAddressSetting = "coinffeine.broker.address"

  private val ConnectionTimeout = Timeout(10.seconds)

  trait Component extends PeerActor.Component {
    this: QuoteRequestActor.Component with OrdersActor.Component
      with MessageGateway.Component with ConfigComponent =>

    override lazy val peerProps: Props = {
      val peerInfo = new PeerInfo(config.getString(HostSetting), config.getInt(PortSetting))
      val brokerAddress = PeerConnection.parse(config.getString(BrokerAddressSetting))
      Props(new DefaultPeerActor(
        peerInfo,
        brokerAddress,
        gatewayProps = messageGatewayProps,
        quoteRequestProps = quoteRequestProps,
        ordersActorProps = ordersActorProps
      ))
    }
  }
}
