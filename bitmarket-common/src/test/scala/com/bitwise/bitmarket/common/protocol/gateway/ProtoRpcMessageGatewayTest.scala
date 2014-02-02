package com.bitwise.bitmarket.common.protocol.gateway

import java.util.Currency
import scala.util.Random

import akka.actor.{ActorRef, Props}
import akka.testkit.{TestActorRef, TestProbe}
import com.googlecode.protobuf.pro.duplex.PeerInfo
import org.scalatest.concurrent.{IntegrationPatience, Eventually}

import com.bitwise.bitmarket.common.{PeerConnection, AkkaSpec}
import com.bitwise.bitmarket.common.currency.{FiatAmount, BtcAmount}
import com.bitwise.bitmarket.common.protocol.{TestClient, OrderMatch}
import com.bitwise.bitmarket.common.protocol.gateway.MessageGateway.ReceiveMessage
import com.bitwise.bitmarket.common.protocol.protobuf.DefaultProtoMappings._
import com.bitwise.bitmarket.common.protocol.protobuf.ProtoMapping.toProtobuf

class ProtoRpcMessageGatewayTest
    extends AkkaSpec("MessageGatewaySystem") with Eventually with IntegrationPatience {

  "Protobuf RPC Message gateway" must "send a known message to a remote peer" in new FreshGateway {
    val msg = makeOrderMatch
    gateway ! MessageGateway.ForwardMessage(msg, remotePeerConnection)
    eventually {
      remotePeer.receivedMessagesNumber should be (1)
      remotePeer.receivedMessages contains toProtobuf(msg)
    }
  }

  it must "send a known message twice reusing the connection to the remote peer" in new FreshGateway {
    val (msg1, msg2) = (makeOrderMatch, makeOrderMatch)
    gateway ! MessageGateway.ForwardMessage(msg1, remotePeerConnection)
    gateway ! MessageGateway.ForwardMessage(msg2, remotePeerConnection)
    eventually {
      remotePeer.receivedMessagesNumber should be (2)
      remotePeer.receivedMessages contains toProtobuf(msg1)
      remotePeer.receivedMessages contains toProtobuf(msg2)
    }
  }

  it must "throw while forwarding when recipient was never connected" in new FreshGateway {
    val msg = makeOrderMatch
    remotePeer.shutdown()
    intercept[MessageGateway.ForwardException] {
      testGateway.receive(MessageGateway.ForwardMessage(msg, remotePeerConnection))
    }
  }

  it must "throw while forwarding when recipient was connected and then disconnects" in new FreshGateway {
    val (msg1, msg2) = (makeOrderMatch, makeOrderMatch)
    testGateway.receive(MessageGateway.ForwardMessage(msg1, remotePeerConnection))
    eventually { remotePeer.receivedMessagesNumber should be (1) }
    remotePeer.shutdown()
    testGateway.receive(MessageGateway.ForwardMessage(msg2, remotePeerConnection))
    eventually {
      remotePeer.receivedMessagesNumber should be (1)
      remotePeer.receivedMessages contains toProtobuf(msg1)
    }
  }

  val subscribeToOrderMatches = MessageGateway.Subscribe {
    case ReceiveMessage(msg: OrderMatch, _) => true
    case _ => false
  }

  it must "deliver messages to subscribers when filter match" in new FreshGateway {
    val msg = makeOrderMatch
    gateway ! subscribeToOrderMatches
    remotePeer.notifyMatchToRemote(toProtobuf(msg))
    expectMsg(ReceiveMessage(msg, remotePeer.connection))
  }

  it must "do not deliver messages to subscribers when filter doesn't match" in new FreshGateway {
    val msg = makeOrderMatch
    gateway ! MessageGateway.Subscribe(msg => false)
    remotePeer.notifyMatchToRemote(toProtobuf(msg))
    expectNoMsg()
  }

  it must "deliver messages to several subscribers when filter match" in new FreshGateway {
    val msg = makeOrderMatch
    val subs = for (i <- 1 to 5) yield TestProbe()
    subs.foreach(_.send(gateway, subscribeToOrderMatches))
    remotePeer.notifyMatchToRemote(toProtobuf(msg))
    subs.foreach(_.expectMsg(ReceiveMessage(msg, remotePeer.connection)))
  }

  trait MessageUtils {

    def makeOrderMatch: OrderMatch = new OrderMatch(
      orderMatchId = s"orderId-${Random.nextLong().toHexString}",
      amount = new BtcAmount(BigDecimal(Random.nextDouble())),
      price = new FiatAmount(BigDecimal(Random.nextDouble()), Currency.getInstance("EUR")),
      buyer = PeerConnection("buyer", randomPort()),
      seller = PeerConnection("seller", randomPort())
    )

    private def randomPort() = Random.nextInt(50000) + 10000
  }

  trait FreshGateway extends MessageUtils {
    val (localPeerAddress, gateway) = createGateway
    val (remotePeerAddress , remotePeer) = createRemotePeer(localPeerAddress)
    val remotePeerConnection = new PeerConnection(
      remotePeerAddress.getHostName, remotePeerAddress.getPort)
    val testGateway = createGatewayTestActor

    private def createGateway: (PeerInfo, ActorRef) = {
      eventually {
        val peerInfo = new PeerInfo("localhost", randomPort())
        (peerInfo, system.actorOf(Props(new ProtoRpcMessageGateway(peerInfo))))
      }
    }

    private def createGatewayTestActor: TestActorRef[ProtoRpcMessageGateway] = {
      eventually {
        val peerInfo = new PeerInfo("localhost", randomPort())
        TestActorRef(new ProtoRpcMessageGateway(peerInfo))
      }
    }

    private def createRemotePeer(localPeerAddress: PeerInfo): (PeerInfo, TestClient) = {
      eventually {
        val peerInfo = new PeerInfo("localhost", randomPort())
        val client = new TestClient(peerInfo.getPort, localPeerAddress)
        client.connectToServer()
        (peerInfo, client)
      }
    }

    private def randomPort() = Random.nextInt(50000) + 10000
  }
}
