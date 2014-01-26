package com.bitwise.bitmarket.broker

import scala.concurrent.duration._
import scala.language.postfixOps

import akka.actor._
import akka.testkit._

import com.bitwise.bitmarket.broker.BrokerActor._
import com.bitwise.bitmarket.common.currency.BtcAmount
import com.bitwise.bitmarket.common.currency.CurrencyCode.{EUR, USD}
import com.bitwise.bitmarket.common.protocol.{OrderMatch, Quote, Ask, Bid}
import com.bitwise.bitmarket.common.AkkaSpec

class DefaultBrokerActorTest
  extends AkkaSpec(AkkaSpec.systemWithLoggingInterception("BrokerSystem")) {

  class WithEurBroker(name: String) {
    val probe = TestProbe()
    val broker = system.actorOf(Props(new DefaultBrokerActor(
      currency = EUR.currency,
      orderExpirationInterval = 1 second
    )), name)
  }

  "A broker" must "keep orders and notify when they cross" in new WithEurBroker("notify-crosses") {
    probe.send(broker, OrderPlacement(Bid(BtcAmount(1), EUR(900), "client1")))
    probe.send(broker, OrderPlacement(Bid(BtcAmount(0.8), EUR(950), "client2")))
    probe.expectNoMsg(100 millis)

    probe.send(broker, OrderPlacement(Ask(BtcAmount(0.6), EUR(850), "client3")))
    val orderMatch = probe.expectMsgClass(classOf[NotifyCross]).cross
    orderMatch.amount should be (BtcAmount(0.6))
    orderMatch.price should be (EUR(900))
    orderMatch.buyer should be ("client2")
    orderMatch.seller should be ("client3")
  }

  it must "quote spreads" in new WithEurBroker("quote-spreads") {
    probe.send(broker, QuoteRequest)
    probe.expectMsg(QuoteResponse(Quote()))
    probe.send(broker, OrderPlacement(Bid(BtcAmount(1), EUR(900), "client1")))
    probe.send(broker, QuoteRequest)
    probe.expectMsg(QuoteResponse(Quote(Some(EUR(900)) -> None)))
    probe.send(broker, OrderPlacement(Ask(BtcAmount(0.8), EUR(950), "client2")))
    probe.send(broker, QuoteRequest)
    probe.expectMsg(QuoteResponse(Quote(Some(EUR(900)) -> Some(EUR(950)))))
  }

  it must "quote last price" in new WithEurBroker("quote-last-price") {
    probe.send(broker, OrderPlacement(Bid(BtcAmount(1), EUR(900), "client1")))
    probe.send(broker, OrderPlacement(Ask(BtcAmount(1), EUR(800), "client2")))
    probe.send(broker, QuoteRequest)
    probe.expectMsgClass(classOf[NotifyCross])
    probe.expectMsg(QuoteResponse(Quote(lastPrice = Some(EUR(850)))))
  }

  it must "reject orders in other currencies" in new WithEurBroker("reject-other-currencies") {
    EventFilter.error(pattern = ".*", occurrences = 1) intercept {
      probe.send(broker, OrderPlacement(Bid(BtcAmount(1), USD(900), "client")))
      probe.expectNoMsg()
    }
  }

  it must "cancel orders" in new WithEurBroker("cancel-orders") {
    probe.send(broker, OrderPlacement(Bid(BtcAmount(1), EUR(900), "client1")))
    probe.send(broker, OrderPlacement(Ask(BtcAmount(0.8), EUR(950), "client2")))
    probe.send(broker, OrderCancellation("client1"))
    probe.send(broker, QuoteRequest)
    probe.expectMsg(QuoteResponse(Quote(None -> Some(EUR(950)))))
  }

  it must "expire old orders" in new WithEurBroker("expire-orders") {
    probe.send(broker, OrderPlacement(Bid(BtcAmount(1), EUR(900), "client")))
    probe.expectNoMsg(2 seconds)
    probe.send(broker, QuoteRequest)
    probe.expectMsg(QuoteResponse(Quote()))
  }

  it must "keep priority of orders when resubmitted" in new WithEurBroker("keep-priority") {
    val firstBid = OrderPlacement(Bid(BtcAmount(1), EUR(900), "first-bid"))
    val secondBid = OrderPlacement(Bid(BtcAmount(1), EUR(900), "second-bid"))
    probe.send(broker, firstBid)
    probe.send(broker, secondBid)
    probe.send(broker, firstBid)
    probe.send(broker, OrderPlacement(Ask(BtcAmount(1), EUR(900), "ask")))
    val orderMatch = probe.expectMsgClass(classOf[NotifyCross]).cross
    orderMatch.buyer should equal ("first-bid")
  }

  it must "label crosses with random identifiers" in new WithEurBroker("random-id") {
    probe.send(broker, OrderPlacement(Bid(BtcAmount(1), EUR(900), "buyer")))
    probe.send(broker, OrderPlacement(Ask(BtcAmount(1), EUR(900), "seller")))
    val id1 = probe.expectMsgClass(classOf[NotifyCross]).cross.id
    probe.send(broker, OrderPlacement(Bid(BtcAmount(1), EUR(900), "buyer")))
    probe.send(broker, OrderPlacement(Ask(BtcAmount(1), EUR(900), "seller")))
    val id2 = probe.expectMsgClass(classOf[NotifyCross]).cross.id
    id1 should not (equal (id2))
  }
}
