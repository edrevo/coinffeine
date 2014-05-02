package com.coinffeine.client.peer

import akka.actor.{ActorRef, ActorSystem, Props}
import akka.testkit.TestProbe
import com.googlecode.protobuf.pro.duplex.PeerInfo

import com.coinffeine.client.peer.QuoteRequestActor.StartRequest
import com.coinffeine.common.{AkkaSpec, MockActor, PeerConnection}
import com.coinffeine.common.MockActor.{MockReceived, MockStarted}
import com.coinffeine.common.currency.CurrencyCode.EUR
import com.coinffeine.common.protocol.gateway.MessageGateway.{Bind, BindingError, BoundTo}
import com.coinffeine.common.protocol.messages.brokerage.QuoteRequest

class PeerActorTest extends AkkaSpec(ActorSystem("PeerActorTest")) {

  val address = new PeerInfo("localhost", 8080)
  val brokerAddress = PeerConnection("host", 8888)
  val gatewayProbe = TestProbe()
  val requestsProbe = TestProbe()
  val peer = system.actorOf(Props(new PeerActor(address, brokerAddress,
    MockActor.props(gatewayProbe), MockActor.props(requestsProbe))))
  var gatewayRef: ActorRef = _

  "A peer" must "start the message gateway" in {
    gatewayRef = gatewayProbe.expectMsgClass(classOf[MockStarted]).ref
  }

  it must "make the message gateway start listening when connecting" in {
    gatewayProbe.expectNoMsg()
    peer ! PeerActor.Connect
    gatewayProbe.expectMsgPF() {
      case MockReceived(_, sender, Bind(`address`)) => sender ! BoundTo(address)
    }
    expectMsg(PeerActor.Connected)
  }

  it must "propagate failures when connecting" in {
    peer ! PeerActor.Connect
    val cause = new Exception("deep cause")
    gatewayProbe.expectMsgPF() {
      case MockReceived(_, sender, Bind(`address`)) => sender ! BindingError(cause)
    }
    expectMsg(PeerActor.ConnectionFailed(cause))
  }

  it must "delegate quote requests" in {
    peer ! QuoteRequest(EUR.currency)
    val requestRef = requestsProbe.expectMsgClass(classOf[MockStarted]).ref
    val expectedInitialization = StartRequest(EUR.currency, gatewayRef, brokerAddress)
    requestsProbe.expectMsg(MockReceived(requestRef, self, expectedInitialization))
  }
}
