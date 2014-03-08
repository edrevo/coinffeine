package com.coinffeine.client.peer

import akka.actor._
import akka.testkit._

import com.coinffeine.common.{PeerConnection, AkkaSpec}
import com.coinffeine.common.currency.CurrencyCode._
import com.coinffeine.common.protocol.messages.brokerage.{Quote, QuoteRequest}

class PeerActorTest extends AkkaSpec(AkkaSpec.systemWithLoggingInterception("PeerTests")) {

  "A peer" must "forward quote requests to the broker" in {
    val broker = PeerConnection("broker")
    val probe = TestProbe()
    val gateway = new GatewayProbe()
    val peer = system.actorOf(Props(new PeerActor(gateway.ref, broker)), "ask-for-quotes")

    gateway.expectSubscription()
    probe.send(peer, QuoteRequest(EUR.currency))
    gateway.expectForwarding(QuoteRequest(EUR.currency), broker)
    gateway.relayMessage(Quote.empty(EUR.currency), broker)
    probe.expectMsg(Quote.empty(EUR.currency))
  }
}
