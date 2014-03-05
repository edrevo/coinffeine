package com.coinffeine.broker

import scala.concurrent.duration._
import scala.language.postfixOps

import akka.actor._
import akka.testkit._

import com.coinffeine.common.{PeerConnection, AkkaSpec}
import com.coinffeine.common.currency.BtcAmount
import com.coinffeine.common.currency.CurrencyCode.{EUR, USD}
import com.coinffeine.common.protocol._
import com.coinffeine.common.protocol.gateway.MessageGateway._
import com.coinffeine.common.protocol.messages.brokerage.{QuoteRequest, Quote, OrderMatch, OrderCancellation}

class BrokerActorTest
  extends AkkaSpec(AkkaSpec.systemWithLoggingInterception("BrokerSystem")) {

  class FakeHandshakeArbiterActor(listener: ActorRef) extends Actor {
    override def receive: Receive = {
      case orderMatch: OrderMatch =>
        listener ! orderMatch
        self ! PoisonPill
    }
  }

  class WithEurBroker(name: String) {
    val arbiterProbe = TestProbe()
    val gateway = TestProbe()
    val broker = system.actorOf(Props(new BrokerActor(
      currency = EUR.currency,
      gateway = gateway.ref,
      handshakeArbiterProps = Props(new FakeHandshakeArbiterActor(arbiterProbe.ref)),
      orderExpirationInterval = 1 second
    )), name)

    def shouldHaveQuote(expectedQuote: Quote) {
      val quoteRequester = PeerConnection("quoteRequester")
      gateway.send(broker, ReceiveMessage(QuoteRequest(EUR.currency), quoteRequester))
      gateway.expectMsg(ForwardMessage(expectedQuote, quoteRequester))
    }
  }

  "A broker" must "subscribe himself to relevant messages" in new WithEurBroker("subscribe") {
    val Subscribe(filter) = gateway.expectMsgClass(classOf[Subscribe])
    val client: PeerConnection = PeerConnection("client1")
    val eurBid = Order(Bid, BtcAmount(1), EUR(1000))
    val dollarBid = Order(Bid, BtcAmount(1), USD(1200))
    filter(ReceiveMessage(eurBid, PeerConnection("client1"))) should be (true)
    filter(ReceiveMessage(dollarBid, PeerConnection("client1"))) should be (false)
    filter(ReceiveMessage(Order(Ask, BtcAmount(1), EUR(600)), client)) should be (true)
    filter(ReceiveMessage(QuoteRequest(EUR.currency), client)) should be (true)
    filter(ReceiveMessage(QuoteRequest(USD.currency), client)) should be (false)
  }

  it must "keep orders and notify both parts and start an arbiter when they cross" in
    new WithEurBroker("notify-crosses") {
      gateway.expectMsgClass(classOf[Subscribe])
      gateway.send(broker, ReceiveMessage(Order(Bid, BtcAmount(1), EUR(900)), PeerConnection("client1")))
      gateway.send(broker, ReceiveMessage(Order(Bid, BtcAmount(0.8), EUR(950)), PeerConnection("client2")))
      gateway.expectNoMsg(100 millis)

      gateway.send(broker, ReceiveMessage(Order(Ask, BtcAmount(0.6), EUR(850)), PeerConnection("client3")))
      val ForwardMessage(match1, buyer) = gateway.expectMsgClass(classOf[ForwardMessage[OrderMatch]])
      val ForwardMessage(match2, seller) = gateway.expectMsgClass(classOf[ForwardMessage[OrderMatch]])
      match1 should equal (match2)
      match1.amount should be (BtcAmount(0.6))
      match1.price should be (EUR(900))
      match1.buyer should be (PeerConnection("client2"))
      match1.seller should be (PeerConnection("client3"))
      arbiterProbe.expectMsg(match1)
    }

  it must "quote spreads" in new WithEurBroker("quote-spreads") {
    gateway.expectMsgClass(classOf[Subscribe])
    shouldHaveQuote(Quote())
    gateway.send(broker, ReceiveMessage(Order(Bid, BtcAmount(1), EUR(900)), PeerConnection("client1")))
    shouldHaveQuote(Quote(Some(EUR(900)) -> None))
    gateway.send(broker, ReceiveMessage(Order(Ask, BtcAmount(0.8), EUR(950)), PeerConnection("client2")))
    shouldHaveQuote(Quote(Some(EUR(900)) -> Some(EUR(950))))
  }

  it must "quote last price" in new WithEurBroker("quote-last-price") {
    gateway.expectMsgClass(classOf[Subscribe])
    gateway.send(broker, ReceiveMessage(Order(Bid, BtcAmount(1), EUR(900)), PeerConnection("client1")))
    gateway.send(broker, ReceiveMessage(Order(Ask, BtcAmount(1), EUR(800)), PeerConnection("client2")))
    gateway.expectMsgClass(classOf[ForwardMessage[OrderMatch]])
    gateway.expectMsgClass(classOf[ForwardMessage[OrderMatch]])
    shouldHaveQuote(Quote(lastPrice = Some(EUR(850))))
  }

  it must "reject orders in other currencies" in new WithEurBroker("reject-other-currencies") {
    gateway.expectMsgClass(classOf[Subscribe])
    EventFilter.error(pattern = ".*", occurrences = 1) intercept {
      gateway.send(broker, ReceiveMessage(Order(Bid, BtcAmount(1), USD(900)), PeerConnection("client")))
      gateway.expectNoMsg()
    }
  }

  it must "cancel orders" in new WithEurBroker("cancel-orders") {
    gateway.expectMsgClass(classOf[Subscribe])
    gateway.send(broker, ReceiveMessage(Order(Bid, BtcAmount(1), EUR(900)), PeerConnection("client1")))
    gateway.send(broker, ReceiveMessage(Order(Ask, BtcAmount(0.8), EUR(950)), PeerConnection("client2")))
    gateway.send(broker, ReceiveMessage(OrderCancellation(EUR.currency), PeerConnection("client1")))
    shouldHaveQuote(Quote(None -> Some(EUR(950))))
  }

  it must "expire old orders" in new WithEurBroker("expire-orders") {
    gateway.expectMsgClass(classOf[Subscribe])
    gateway.send(broker, ReceiveMessage(Order(Bid, BtcAmount(1), EUR(900)), PeerConnection("client")))
    gateway.expectNoMsg(2 seconds)
    shouldHaveQuote(Quote())
  }

  it must "keep priority of orders when resubmitted" in new WithEurBroker("keep-priority") {
    gateway.expectMsgClass(classOf[Subscribe])
    val firstBid = ReceiveMessage(Order(Bid, BtcAmount(1), EUR(900)), PeerConnection("first-bid"))
    val secondBid = ReceiveMessage(Order(Bid, BtcAmount(1), EUR(900)), PeerConnection("second-bid"))
    gateway.send(broker, firstBid)
    gateway.send(broker, secondBid)
    gateway.send(broker, firstBid)
    gateway.send(broker, ReceiveMessage(Order(Ask, BtcAmount(1), EUR(900)), PeerConnection("ask")))
    val ForwardMessage(orderMatch, _) = gateway.expectMsgClass(classOf[ForwardMessage[OrderMatch]])
    orderMatch.buyer should equal (PeerConnection("first-bid"))
  }

  it must "label crosses with random identifiers" in new WithEurBroker("random-id") {
    gateway.expectMsgClass(classOf[Subscribe])
    gateway.send(broker, ReceiveMessage(Order(Bid, BtcAmount(1), EUR(900)), PeerConnection("buyer")))
    gateway.send(broker, ReceiveMessage(Order(Ask, BtcAmount(1), EUR(900)), PeerConnection("seller")))
    val id1 = gateway.expectMsgClass(classOf[ForwardMessage[OrderMatch]]).msg.exchangeId
    gateway.expectMsgClass(classOf[ForwardMessage[OrderMatch]])
    gateway.send(broker, ReceiveMessage(Order(Bid, BtcAmount(1), EUR(900)), PeerConnection("buyer")))
    gateway.send(broker, ReceiveMessage(Order(Ask, BtcAmount(1), EUR(900)), PeerConnection("seller")))
    val id2 = gateway.expectMsgClass(classOf[ForwardMessage[OrderMatch]]).msg.exchangeId
    gateway.expectMsgClass(classOf[ForwardMessage[OrderMatch]])
    id1 should not (equal (id2))
  }
}
