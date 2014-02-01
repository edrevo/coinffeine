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
    val gateway = TestProbe()
    val broker = system.actorOf(Props(new DefaultBrokerActor(
      currency = EUR.currency,
      gateway = gateway.ref,
      orderExpirationInterval = 1 second
    )), name)
    val quoteRequest = ReceiveMessage(QuoteRequest(EUR.currency), PeerConnection("someone"))
  }

  "A broker" must "keep orders and notify when they cross" in new WithEurBroker("notify-crosses") {
    gateway.send(broker, ReceiveMessage(Bid(BtcAmount(1), EUR(900), PeerConnection("client1")), PeerConnection("client1")))
    gateway.send(broker, ReceiveMessage(Bid(BtcAmount(0.8), EUR(950), PeerConnection("client2")), PeerConnection("client2")))
    gateway.expectNoMsg(100 millis)

    gateway.send(broker, ReceiveMessage(Ask(BtcAmount(0.6), EUR(850), PeerConnection("client3")), PeerConnection("client3")))
    val orderMatch = gateway.expectMsgClass(classOf[OrderMatch])
    orderMatch.amount should be (BtcAmount(0.6))
    orderMatch.price should be (EUR(900))
    orderMatch.buyer should be (PeerConnection("client2"))
    orderMatch.seller should be (PeerConnection("client3"))
  }

  it must "quote spreads" in new WithEurBroker("quote-spreads") {
    gateway.send(broker, quoteRequest)
    gateway.expectMsg(Quote())
    gateway.send(broker, ReceiveMessage(Bid(BtcAmount(1), EUR(900), PeerConnection("client1")), PeerConnection("client1")))
    gateway.send(broker, quoteRequest)
    gateway.expectMsg(Quote(Some(EUR(900)) -> None))
    gateway.send(broker, ReceiveMessage(Ask(BtcAmount(0.8), EUR(950), PeerConnection("client2")), PeerConnection("client2")))
    gateway.send(broker, quoteRequest)
    gateway.expectMsg(Quote(Some(EUR(900)) -> Some(EUR(950))))
  }

  it must "quote last price" in new WithEurBroker("quote-last-price") {
    gateway.send(broker, ReceiveMessage(Bid(BtcAmount(1), EUR(900), PeerConnection("client1")), PeerConnection("client1")))
    gateway.send(broker, ReceiveMessage(Ask(BtcAmount(1), EUR(800), PeerConnection("client2")), PeerConnection("client2")))
    gateway.send(broker, quoteRequest)
    gateway.expectMsgClass(classOf[OrderMatch])
    gateway.expectMsg(Quote(lastPrice = Some(EUR(850))))
  }

  it must "reject orders in other currencies" in new WithEurBroker("reject-other-currencies") {
    EventFilter.error(pattern = ".*", occurrences = 1) intercept {
      gateway.send(broker, ReceiveMessage(Bid(BtcAmount(1), USD(900), PeerConnection("client")), PeerConnection("client")))
      gateway.expectNoMsg()
    }
  }

  it must "cancel orders" in new WithEurBroker("cancel-orders") {
    gateway.send(broker, ReceiveMessage(Bid(BtcAmount(1), EUR(900), PeerConnection("client1")), PeerConnection("client1")))
    gateway.send(broker, ReceiveMessage(Ask(BtcAmount(0.8), EUR(950), PeerConnection("client2")), PeerConnection("client2")))
    gateway.send(broker, ReceiveMessage(OrderCancellation(EUR.currency), PeerConnection("client1")))
    gateway.send(broker, quoteRequest)
    gateway.expectMsg(Quote(None -> Some(EUR(950))))
  }

  it must "expire old orders" in new WithEurBroker("expire-orders") {
    gateway.send(broker, ReceiveMessage(Bid(BtcAmount(1), EUR(900), PeerConnection("client")), PeerConnection("client")))
    gateway.expectNoMsg(2 seconds)
    gateway.send(broker, quoteRequest)
    gateway.expectMsg(Quote())
  }

  it must "keep priority of orders when resubmitted" in new WithEurBroker("keep-priority") {
    val firstBid = ReceiveMessage(Bid(BtcAmount(1), EUR(900), PeerConnection("first-bid")), PeerConnection("first-bid"))
    val secondBid = ReceiveMessage(Bid(BtcAmount(1), EUR(900), PeerConnection("second-bid")), PeerConnection("second-bid"))
    gateway.send(broker, firstBid)
    gateway.send(broker, secondBid)
    gateway.send(broker, firstBid)
    gateway.send(broker, ReceiveMessage(Ask(BtcAmount(1), EUR(900), PeerConnection("ask")), PeerConnection("ask")))
    val orderMatch = gateway.expectMsgClass(classOf[OrderMatch])
    orderMatch.buyer should equal (PeerConnection("first-bid"))
  }

  it must "label crosses with random identifiers" in new WithEurBroker("random-id") {
    gateway.send(broker, ReceiveMessage(Bid(BtcAmount(1), EUR(900), PeerConnection("buyer")), PeerConnection("buyer")))
    gateway.send(broker, ReceiveMessage(Ask(BtcAmount(1), EUR(900), PeerConnection("seller")), PeerConnection("seller")))
    val id1 = gateway.expectMsgClass(classOf[OrderMatch]).orderMatchId
    gateway.send(broker, ReceiveMessage(Bid(BtcAmount(1), EUR(900), PeerConnection("buyer")), PeerConnection("buyer")))
    gateway.send(broker, ReceiveMessage(Ask(BtcAmount(1), EUR(900), PeerConnection("seller")), PeerConnection("seller")))
    val id2 = gateway.expectMsgClass(classOf[OrderMatch]).orderMatchId
    id1 should not (equal (id2))
  }
}
