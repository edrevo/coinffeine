package com.coinffeine.client.peer

import akka.actor._

import com.coinffeine.client.peer.PeerActor.JoinNetwork
import com.coinffeine.common.{AkkaSpec, PeerConnection}
import com.coinffeine.common.currency.CurrencyCode._
import com.coinffeine.common.protocol.gateway.GatewayProbe
import com.coinffeine.common.protocol.messages.brokerage.{Quote, QuoteRequest}

class PeerActorTest extends AkkaSpec(ActorSystem("PeerTests")) {

  val broker = PeerConnection("broker")
  val gateway = new GatewayProbe()
  val peer = system.actorOf(Props(new PeerActor()), "ask-for-quotes")

  "A peer" should "subscribe to messages when joining the network" in {
    gateway.expectNoMsg()
    peer ! JoinNetwork(gateway.ref, broker)
    gateway.expectSubscription()
  }

  it should "forward quote requests to the broker" in {
    peer ! QuoteRequest(EUR.currency)
    gateway.expectForwarding(QuoteRequest(EUR.currency), broker)
    gateway.relayMessage(Quote.empty(EUR.currency), broker)
    expectMsg(Quote.empty(EUR.currency))
  }
}
