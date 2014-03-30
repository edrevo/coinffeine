package com.coinffeine.common.protocol

import com.google.protobuf.{Message => ProtoMessage, RpcCallback, RpcController}
import com.googlecode.protobuf.pro.duplex.PeerInfo

import com.coinffeine.common.PeerConnection
import com.coinffeine.common.protocol.protobuf.{CoinffeineProtobuf => msg}
import com.coinffeine.common.protorpc.{PeerSession, PeerServer}
import com.coinffeine.common.protocol.messages.PublicMessage
import com.coinffeine.common.protocol.serialization.ProtocolSerialization

class TestClient(
    port: Int,
    serverInfo: PeerInfo,
    serialization: ProtocolSerialization) extends msg.PeerService.Interface {
  val info = new PeerInfo("localhost", port)
  val connection = PeerConnection(info.getHostName, port)
  var sessionOption: Option[PeerSession] = None

  @volatile
  private var receivedMessages_ : Seq[ProtoMessage] = Seq.empty
  private val server = new PeerServer(info, msg.PeerService.newReflectiveService(this))

  server.start().await

  def receivedMessages: Seq[ProtoMessage] = synchronized { receivedMessages_ }

  def receivedMessagesNumber: Int = receivedMessages.size

  def shutdown(): Unit = {
    disconnect()
    server.shutdown()
  }

  def connectToServer(): Unit = {
    sessionOption = Some(server.peerWith(serverInfo).get)
  }

  def disconnect(): Unit = {
    sessionOption.foreach(_.close())
    sessionOption = None
  }

  def sendMessage(message: PublicMessage): Unit = {
    val session = sessionOption.get
    val stub = msg.PeerService.newBlockingStub(session.channel)
    stub.sendMessage(session.controller, serialization.toProtobuf(message))
  }

  override def sendMessage(
      c: RpcController, request: msg.CoinffeineMessage, done: RpcCallback[msg.Void]): Unit = {
    synchronized {
      receivedMessages_ = receivedMessages_ :+ request
    }
    done.run(msg.Void.getDefaultInstance)
  }
}
