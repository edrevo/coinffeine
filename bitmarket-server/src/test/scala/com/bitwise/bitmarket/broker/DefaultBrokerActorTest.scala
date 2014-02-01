package com.bitwise.bitmarket.broker

import scala.concurrent.duration._
import scala.language.postfixOps

import akka.actor._
import akka.testkit._

import com.bitwise.bitmarket.common.{PeerConnection, AkkaSpec}
import com.bitwise.bitmarket.common.currency.BtcAmount
import com.bitwise.bitmarket.common.currency.CurrencyCode.{EUR, USD}
import com.bitwise.bitmarket.common.protocol._
import com.bitwise.bitmarket.common.protocol.gateway.MessageGateway.ReceiveMessage

class DefaultBrokerActorTest
  extends AkkaSpec(AkkaSpec.systemWithLoggingInterception("BrokerSystem")) {

  class WithEurBroker(name: String) {
    val probe = TestProbe()
    val broker = system.actorOf(Props(new DefaultBrokerActor(
      currency = EUR.currency,
      orderExpirationInterval = 1 second
    )), name)
    val quoteRequest = QuoteRequest(EUR.currency)
  }

  "A broker" must "keep orders and notify when they cross" in new WithEurBroker("notify-crosses") {
    probe.send(broker, Bid(BtcAmount(1), EUR(900), PeerConnection("client1")))
    probe.send(broker, Bid(BtcAmount(0.8), EUR(950), PeerConnection("client2")))
    probe.expectNoMsg(100 millis)

    probe.send(broker, Ask(BtcAmount(0.6), EUR(850), PeerConnection("client3")))
    val orderMatch = probe.expectMsgClass(classOf[OrderMatch])
    orderMatch.amount should be (BtcAmount(0.6))
    orderMatch.price should be (EUR(900))
    orderMatch.buyer should be (PeerConnection("client2"))
    orderMatch.seller should be (PeerConnection("client3"))
  }

  it must "quote spreads" in new WithEurBroker("quote-spreads") {
    probe.send(broker, quoteRequest)
    probe.expectMsg(Quote())
    probe.send(broker, Bid(BtcAmount(1), EUR(900), PeerConnection("client1")))
    probe.send(broker, quoteRequest)
    probe.expectMsg(Quote(Some(EUR(900)) -> None))
    probe.send(broker, Ask(BtcAmount(0.8), EUR(950), PeerConnection("client2")))
    probe.send(broker, quoteRequest)
    probe.expectMsg(Quote(Some(EUR(900)) -> Some(EUR(950))))
  }

  it must "quote last price" in new WithEurBroker("quote-last-price") {
    probe.send(broker, Bid(BtcAmount(1), EUR(900), PeerConnection("client1")))
    probe.send(broker, Ask(BtcAmount(1), EUR(800), PeerConnection("client2")))
    probe.send(broker, quoteRequest)
    probe.expectMsgClass(classOf[OrderMatch])
    probe.expectMsg(Quote(lastPrice = Some(EUR(850))))
  }

  it must "reject orders in other currencies" in new WithEurBroker("reject-other-currencies") {
    EventFilter.error(pattern = ".*", occurrences = 1) intercept {
      probe.send(broker, Bid(BtcAmount(1), USD(900), PeerConnection("client")))
      probe.expectNoMsg()
    }
  }

  it must "cancel orders" in new WithEurBroker("cancel-orders") {
    probe.send(broker, Bid(BtcAmount(1), EUR(900), PeerConnection("client1")))
    probe.send(broker, Ask(BtcAmount(0.8), EUR(950), PeerConnection("client2")))
    probe.send(broker, ReceiveMessage(OrderCancellation(EUR.currency), PeerConnection("client1")))
    probe.send(broker, quoteRequest)
    probe.expectMsg(Quote(None -> Some(EUR(950))))
  }

  it must "expire old orders" in new WithEurBroker("expire-orders") {
    probe.send(broker, Bid(BtcAmount(1), EUR(900), PeerConnection("client")))
    probe.expectNoMsg(2 seconds)
    probe.send(broker, quoteRequest)
    probe.expectMsg(Quote())
  }

  it must "keep priority of orders when resubmitted" in new WithEurBroker("keep-priority") {
    val firstBid = Bid(BtcAmount(1), EUR(900), PeerConnection("first-bid"))
    val secondBid = Bid(BtcAmount(1), EUR(900), PeerConnection("second-bid"))
    probe.send(broker, firstBid)
    probe.send(broker, secondBid)
    probe.send(broker, firstBid)
    probe.send(broker, Ask(BtcAmount(1), EUR(900), PeerConnection("ask")))
    val orderMatch = probe.expectMsgClass(classOf[OrderMatch])
    orderMatch.buyer should equal (PeerConnection("first-bid"))
  }

  it must "label crosses with random identifiers" in new WithEurBroker("random-id") {
    probe.send(broker, Bid(BtcAmount(1), EUR(900), PeerConnection("buyer")))
    probe.send(broker, Ask(BtcAmount(1), EUR(900), PeerConnection("seller")))
    val id1 = probe.expectMsgClass(classOf[OrderMatch]).orderMatchId
    probe.send(broker, Bid(BtcAmount(1), EUR(900), PeerConnection("buyer")))
    probe.send(broker, Ask(BtcAmount(1), EUR(900), PeerConnection("seller")))
    val id2 = probe.expectMsgClass(classOf[OrderMatch]).orderMatchId
    id1 should not (equal (id2))
  }
}
