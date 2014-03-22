package com.coinffeine.common.protocol.gateway

import java.io.IOException

import akka.actor.{Props, Terminated, ActorRef, Actor}
import com.google.protobuf.{RpcCallback, RpcController}
import com.googlecode.protobuf.pro.duplex.PeerInfo
import com.googlecode.protobuf.pro.duplex.execute.ServerRpcController

import com.coinffeine.common.PeerConnection
import com.coinffeine.common.protocol.gateway.MessageGateway._
import com.coinffeine.common.protocol.protobuf.{CoinffeineProtobuf => proto}
import com.coinffeine.common.protorpc.{Callbacks, PeerSession, PeerServer}
import com.coinffeine.common.protocol.messages.PublicMessage
import com.coinffeine.common.protocol.serialization.{ProtocolSerializationComponent, ProtocolSerialization}

private[gateway] class ProtoRpcMessageGateway(
   serverInfo: PeerInfo, serialization: ProtocolSerialization) extends Actor {

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

  private val server: PeerServer = new PeerServer(
    serverInfo, proto.PeerService.newReflectiveService(new PeerServiceImpl()))

  private var subscriptions = Map.empty[ActorRef, MessageSubscription]
  private var sessions = Map.empty[PeerConnection, PeerSession]

  override def preStart(): Unit = {
    val starting = server.start()
    starting.await()
    if (!starting.isSuccess) {
      server.shutdown()
      throw starting.cause()
    }
  }

  override def postStop(): Unit = server.shutdown()

  override def receive = {
    case m @ ForwardMessage(msg, dest) =>
      forward(sender, dest, msg)
    case Subscribe(filter) =>
      subscriptions += sender -> MessageSubscription(filter)
    case Unsubscribe =>
      subscriptions -= sender
    case Terminated(actor) =>
      subscriptions -= actor
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

  trait Component extends MessageGateway.Component { this: ProtocolSerializationComponent =>
    override def messageGatewayProps(serverInfo: PeerInfo): Props =
      Props(new ProtoRpcMessageGateway(serverInfo, protocolSerialization))
  }
}
