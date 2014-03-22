package com.coinffeine.common.protocol.gateway

import scala.concurrent.duration._

import akka.actor.ActorRef
import akka.testkit.{TestActorRef, TestProbe}
import com.googlecode.protobuf.pro.duplex.PeerInfo
import org.scalatest.concurrent.{IntegrationPatience, Eventually}

import com.coinffeine.common.{DefaultTcpPortAllocator, PeerConnection, AkkaSpec}
import com.coinffeine.common.protocol.TestClient
import com.coinffeine.common.protocol.serialization._
import com.coinffeine.common.protocol.messages.brokerage.OrderMatch
import com.coinffeine.common.protocol.gateway.MessageGateway.ReceiveMessage

class ProtoRpcMessageGatewayTest extends AkkaSpec("MessageGatewaySystem")
  with Eventually with IntegrationPatience {

  val receiveTimeout = 10.seconds

  "Protobuf RPC Message gateway" must "send a known message to a remote peer" in new FreshGateway {
    val (message, protoMessage) = randomMessageAndSerialization()
    gateway ! MessageGateway.ForwardMessage(message, remotePeerConnection)
    eventually {
      remotePeer.receivedMessagesNumber should be (1)
      remotePeer.receivedMessages contains protoMessage
    }
  }

  it must "send a known message twice reusing the connection to the remote peer" in
    new FreshGateway {
      val (msg1, msg2) = (randomMessage(), randomMessage())
      gateway ! MessageGateway.ForwardMessage(msg1, remotePeerConnection)
      gateway ! MessageGateway.ForwardMessage(msg2, remotePeerConnection)
      eventually {
        remotePeer.receivedMessagesNumber should be (2)
        remotePeer.receivedMessages contains protocolSerialization.toProtobuf(msg1)
        remotePeer.receivedMessages contains protocolSerialization.toProtobuf(msg2)
      }
    }

  it must "throw while forwarding when recipient was never connected" in new FreshGateway {
    val msg = randomMessage()
    remotePeer.shutdown()
    intercept[MessageGateway.ForwardException] {
      testGateway.receive(MessageGateway.ForwardMessage(msg, remotePeerConnection))
    }
  }

  it must "throw while forwarding when recipient was connected and then disconnects" in
    new FreshGateway {
      val (msg1, msg2) = (randomMessage(), randomMessage())
      testGateway.receive(MessageGateway.ForwardMessage(msg1, remotePeerConnection))
      eventually { remotePeer.receivedMessagesNumber should be (1) }
      remotePeer.shutdown()
      testGateway.receive(MessageGateway.ForwardMessage(msg2, remotePeerConnection))
      eventually {
        remotePeer.receivedMessagesNumber should be (1)
        remotePeer.receivedMessages contains protocolSerialization.toProtobuf(msg1)
      }
    }

  val subscribeToOrderMatches = MessageGateway.Subscribe {
    case ReceiveMessage(msg: OrderMatch, _) => true
    case _ => false
  }

  it must "deliver messages to subscribers when filter match" in new FreshGateway {
    val msg = randomMessage()
    gateway ! subscribeToOrderMatches
    remotePeer.sendMessage(msg)
    expectMsg(receiveTimeout, ReceiveMessage(msg, remotePeer.connection))
  }

  it must "do not deliver messages to subscribers when filter doesn't match" in new FreshGateway {
    val msg = randomMessage()
    gateway ! MessageGateway.Subscribe(msg => false)
    remotePeer.sendMessage(msg)
    expectNoMsg()
  }

  it must "deliver messages to several subscribers when filter match" in new FreshGateway {
    val msg = randomMessage()
    val subs = for (i <- 1 to 5) yield TestProbe()
    subs.foreach(_.send(gateway, subscribeToOrderMatches))
    remotePeer.sendMessage(msg)
    subs.foreach(_.expectMsg(receiveTimeout, ReceiveMessage(msg, remotePeer.connection)))
  }

  trait FreshGateway extends ProtoRpcMessageGateway.Component
      with TestProtocolSerializationComponent {
    val (localPeerAddress, gateway) = createGateway()
    val (remotePeerAddress, remotePeer) = createRemotePeer(localPeerAddress)
    val remotePeerConnection = new PeerConnection(
      remotePeerAddress.getHostName, remotePeerAddress.getPort)
    val testGateway = createGatewayTestActor

    private def createGateway(): (PeerInfo, ActorRef) = {
      val peerInfo = allocateLocalPeerInfo()
      eventually {
        (peerInfo, system.actorOf(messageGatewayProps(peerInfo)))
      }
    }

    private def createGatewayTestActor: TestActorRef[ProtoRpcMessageGateway] = {
      val peerInfo = allocateLocalPeerInfo()
      eventually {
        TestActorRef(new ProtoRpcMessageGateway(peerInfo, protocolSerialization))
      }
    }

    private def createRemotePeer(localPeerAddress: PeerInfo): (PeerInfo, TestClient) = {
      val peerInfo = allocateLocalPeerInfo()
      eventually {
        val client = new TestClient(peerInfo.getPort, localPeerAddress, protocolSerialization)
        client.connectToServer()
        (peerInfo, client)
      }
    }

    private def allocateLocalPeerInfo() =
      new PeerInfo("localhost", DefaultTcpPortAllocator.allocatePort())
  }
}
