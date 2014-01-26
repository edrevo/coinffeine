package com.bitwise.bitmarket.common.protocol

import java.io.IOException

import akka.actor.{Terminated, ActorRef, Actor}
import com.google.protobuf.{RpcCallback, RpcController}
import com.googlecode.protobuf.pro.duplex.PeerInfo

import com.bitwise.bitmarket.common.PeerConnection
import com.bitwise.bitmarket.common.protocol.protobuf.{
  ProtobufConversions, BitmarketProtobuf => proto}
import com.bitwise.bitmarket.common.protorpc.{Callbacks, PeerSession, PeerServer}

class ProtoRpcMessageGateway(serverInfo: PeerInfo) extends Actor {

  import MessageGateway._
  import ProtoRpcMessageGateway._

  /** Metadata on message subscription requested by an actor. */
  private case class MessageSubscription(filter: Any => Boolean)

  private class PeerServiceImpl extends proto.PeerService.Interface {

    override def notifyMatch(
        controller: RpcController,
        request: proto.OrderMatch,
        done: RpcCallback[proto.Void]) {
      dispatchToSubscriptions(ProtobufConversions.fromProtobuf(request))
      done.run(VoidResponse)
    }

    override def publish(
        controller: RpcController,
        request: proto.Offer,
        done: RpcCallback[proto.PublishResponse]) {
      dispatchToSubscriptions(ProtobufConversions.fromProtobuf(request))
      done.run(SuccessPublishResponse)
    }

    override def requestExchange(
        controller: RpcController,
        request: proto.ExchangeRequest,
        done: RpcCallback[proto.ExchangeRequestResponse]) {
      dispatchToSubscriptions(ProtobufConversions.fromProtobuf(request))
      done.run(SuccessExchangeResponse)
    }
  }

  private val server: PeerServer = new PeerServer(
    serverInfo, proto.PeerService.newReflectiveService(new PeerServiceImpl()))

  private var subscriptions = Map.empty[ActorRef, MessageSubscription]
  private var sessions = Map.empty[PeerConnection, PeerSession]

  override def preStart() {
    val starting = server.start()
    starting.await()
    if (!starting.isSuccess) {
      server.shutdown()
      throw starting.cause()
    }
  }

  override def postStop() {
    server.shutdown()
  }

  override def receive = {
    case ForwardMessage(msg, dest) =>
      forward(sender, dest, msg)
    case Subscribe(filter) =>
      subscriptions += sender -> MessageSubscription(filter)
    case Unsubscribe =>
      subscriptions -= sender
    case Terminated(actor) =>
      subscriptions -= actor
  }

  private def forward(from: ActorRef, to: PeerConnection, msg: Any) {
    try {
      val sess = session(to)
      val srv = proto.PeerService.newStub(sess.channel)
      msg match {
        case msg: OrderMatch =>
          srv.notifyMatch(
            sess.controller,
            ProtobufConversions.toProtobuf(msg),
            Callbacks.noop[proto.Void])
        case msg: Offer =>
          srv.publish(
            sess.controller,
            ProtobufConversions.toProtobuf(msg),
            Callbacks.noop[proto.PublishResponse])
        case msg: ExchangeRequest =>
          srv.requestExchange(
            sess.controller,
            ProtobufConversions.toProtobuf(msg),
            Callbacks.noop[proto.ExchangeRequestResponse])
        case _ =>
          throw ForwardException(
            s"cannot forward unknown message $msg: no forward mechanism defined")
      }
    } catch {
      case e: IOException =>
        throw ForwardException(s"cannot forward message $msg to $to: ${e.getMessage}", e)
    }
  }

  private def dispatchToSubscriptions(msg: Any) {
    for ((actor, MessageSubscription(filter)) <- subscriptions if filter(msg)) {
      actor ! msg
    }
  }

  private def session(connection: PeerConnection): PeerSession = {
    sessions.getOrElse(connection, {
      val sess = server.peerWith(new PeerInfo(connection.hostname, connection.port)).get
      sessions += connection -> sess
      sess
    })
  }
}

object ProtoRpcMessageGateway {

  private[protocol] val VoidResponse = proto.Void.newBuilder().build()
  private[protocol] val SuccessPublishResponse = proto.PublishResponse.newBuilder()
    .setResult(proto.PublishResponse.Result.SUCCESS)
    .build()
  private[protocol] val SuccessExchangeResponse = proto.ExchangeRequestResponse.newBuilder()
    .setResult(proto.ExchangeRequestResponse.Result.SUCCESS)
    .build()
}
