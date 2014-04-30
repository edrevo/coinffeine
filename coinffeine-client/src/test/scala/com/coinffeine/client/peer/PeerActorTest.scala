package com.coinffeine.client.peer

import akka.actor.{ActorRef, ActorSystem, Props}
import akka.testkit.TestProbe
import com.googlecode.protobuf.pro.duplex.PeerInfo

import com.coinffeine.common.{AkkaSpec, MockActor, PeerConnection}
import com.coinffeine.common.MockActor.{MockReceived, MockStarted}
import com.coinffeine.common.protocol.gateway.MessageGateway.Bind
import com.coinffeine.common.protocol.messages.brokerage.QuoteRequest
import com.coinffeine.common.currency.CurrencyCode.EUR
import com.coinffeine.client.peer.QuoteRequestActor.StartRequest

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
    gatewayProbe.expectMsg(MockReceived(gatewayRef, peer, Bind(address)))
  }

  it must "delegate quote requests" in {
    peer ! QuoteRequest(EUR.currency)
    val requestRef = requestsProbe.expectMsgClass(classOf[MockStarted]).ref
    val expectedInitialization = StartRequest(EUR.currency, gatewayRef, brokerAddress)
    requestsProbe.expectMsg(MockReceived(requestRef, self, expectedInitialization))
  }
}
