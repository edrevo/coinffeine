package com.coinffeine.common.protocol.gateway

import java.io.IOException

import akka.actor._
import com.google.protobuf.{RpcCallback, RpcController}
import com.googlecode.protobuf.pro.duplex.PeerInfo
import com.googlecode.protobuf.pro.duplex.execute.ServerRpcController
import io.netty.channel.ChannelFuture
import io.netty.util.concurrent.{Future, GenericFutureListener}

import com.coinffeine.common.PeerConnection
import com.coinffeine.common.network.NetworkComponent
import com.coinffeine.common.protocol.gateway.MessageGateway._
import com.coinffeine.common.protocol.messages.PublicMessage
import com.coinffeine.common.protocol.protobuf.{CoinffeineProtobuf => proto}
import com.coinffeine.common.protocol.serialization.{ProtocolSerialization, ProtocolSerializationComponent}
import com.coinffeine.common.protorpc.{Callbacks, PeerServer, PeerSession}

private[gateway] class ProtoRpcMessageGateway(serialization: ProtocolSerialization)
  extends Actor with ActorLogging {

  import ProtoRpcMessageGateway._

  /** Metadata on message subscription requested by an actor. */
  private case class MessageSubscription(filter: Filter)

  private class PeerServiceImpl extends proto.PeerService.Interface {

    override def sendMessage(
        controller: RpcController,
        message: proto.CoinffeineMessage,
        done: RpcCallback[proto.Void]): Unit = {
      dispatchToSubscriptions(serialization.fromProtobuf(message), clientPeerConnection(controller))
      done.run(VoidResponse)
    }

    private def clientPeerConnection(controller: RpcController) = {
      val info = controller.asInstanceOf[ServerRpcController].getRpcClient.getServerInfo
      PeerConnection(info.getHostName, info.getPort)
    }
  }

  private var server: PeerServer = _
  private var serverInfo: PeerInfo = _
  private var subscriptions = Map.empty[ActorRef, MessageSubscription]
  private var sessions = Map.empty[PeerConnection, PeerSession]

  override def postStop(): Unit = server.shutdown()

  override def receive = waitingForInitialization orElse managingSubscriptions

  private val forwardingMessages: Receive = {
    case m @ ForwardMessage(msg, dest) =>
      forward(sender, dest, msg)
  }

  private val managingSubscriptions: Receive = {
    case Subscribe(filter) =>
      subscriptions += sender -> MessageSubscription(filter)
    case Unsubscribe =>
      subscriptions -= sender
    case Terminated(actor) =>
      subscriptions -= actor
  }

  private def binding(startFuture: ChannelFuture, listener: ActorRef): Receive = {
    case ServerStarted if startFuture.isSuccess =>
      listener ! BoundTo(serverInfo)
      context.become(forwardingMessages orElse managingSubscriptions)
      log.info(s"Message gateway started on $serverInfo")
    case ServerStarted =>
      server.shutdown()
      listener ! BindingError(startFuture.cause())
      log.info(s"Message gateway couldn't start at $serverInfo")
      context.become(waitingForInitialization orElse managingSubscriptions)
  }

  private val waitingForInitialization: Receive = {
    case Bind(address) =>
      val startFuture = startServer(address)
      context.become(binding(startFuture, sender) orElse managingSubscriptions)
  }

  private def forward(from: ActorRef, to: PeerConnection, message: PublicMessage): Unit =
    try {
      val s = session(to)
      proto.PeerService.newStub(s.channel).sendMessage(
        s.controller,
        serialization.toProtobuf(message),
        Callbacks.noop[proto.Void]
      )
    } catch {
      case e: IOException =>
        throw ForwardException(s"cannot forward message $message to $to: ${e.getMessage}", e)
    }

  private def startServer(address: PeerInfo): ChannelFuture = {
    serverInfo = address
    server = new PeerServer(serverInfo, proto.PeerService.newReflectiveService(new PeerServiceImpl()))
    val starting = server.start()
    starting.addListener(new GenericFutureListener[Future[_ >: Void]] {
      override def operationComplete(future: Future[_ >: Void]): Unit = self ! ServerStarted
    })
  }

  private def dispatchToSubscriptions(msg: PublicMessage, sender: PeerConnection): Unit = {
    val notification = ReceiveMessage(msg, sender)
    for ((actor, MessageSubscription(filter)) <- subscriptions if filter(notification)) {
      actor ! notification
    }
  }

  private def session(connection: PeerConnection): PeerSession = sessions.getOrElse(connection, {
    val s = server.peerWith(new PeerInfo(connection.hostname, connection.port)).get
    sessions += connection -> s
    s
  })
}

object ProtoRpcMessageGateway {

  private[protocol] val VoidResponse = proto.Void.newBuilder().build()

  private case object ServerStarted

  trait Component extends MessageGateway.Component {
    this: ProtocolSerializationComponent with NetworkComponent=>

    override lazy val messageGatewayProps = Props(new ProtoRpcMessageGateway(protocolSerialization))
  }
}
