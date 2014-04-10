package com.coinffeine.broker

import scala.concurrent.duration._

import akka.actor._
import akka.testkit._

import com.coinffeine.broker.BrokerActor.StartBrokering
import com.coinffeine.common.{AkkaSpec, PeerConnection}
import com.coinffeine.common.currency.BtcAmount
import com.coinffeine.common.currency.CurrencyCode.{EUR, USD}
import com.coinffeine.common.protocol.gateway.GatewayProbe
import com.coinffeine.common.protocol.gateway.MessageGateway._
import com.coinffeine.common.protocol.messages.brokerage._

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
    val gateway = new GatewayProbe()
    val broker = system.actorOf(Props(new BrokerActor(
      handshakeArbiterProps = Props(new FakeHandshakeArbiterActor(arbiterProbe.ref)),
      orderExpirationInterval = 1 second
    )), name)

    def shouldHaveQuote(expectedQuote: Quote): Unit = {
      val quoteRequester = PeerConnection("quoteRequester")
      gateway.relayMessage(QuoteRequest(EUR.currency), quoteRequester)
      gateway.expectForwarding(expectedQuote, quoteRequester)
    }

    def shouldSubscribe(): Subscribe = {
      broker ! StartBrokering(EUR.currency, gateway.ref)
      gateway.expectSubscription()
    }

    def shouldSpawnArbiter() = arbiterProbe.expectMsgClass(classOf[OrderMatch])
  }

  "A broker" must "subscribe himself to relevant messages" in new WithEurBroker("subscribe") {
    val Subscribe(filter) = shouldSubscribe()
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
      shouldSubscribe()
      gateway.relayMessage(Order(Bid, BtcAmount(1), EUR(900)), PeerConnection("client1"))
      gateway.relayMessage(Order(Bid, BtcAmount(0.8), EUR(950)), PeerConnection("client2"))
      gateway.relayMessage(Order(Ask, BtcAmount(0.6), EUR(850)), PeerConnection("client3"))
      val notifiedOrderMatch = arbiterProbe.expectMsgClass(classOf[OrderMatch])
      notifiedOrderMatch.amount should be (BtcAmount(0.6))
      notifiedOrderMatch.price should be (EUR(900))
      notifiedOrderMatch.buyer should be (PeerConnection("client2"))
      notifiedOrderMatch.seller should be (PeerConnection("client3"))
    }

  it must "quote spreads" in new WithEurBroker("quote-spreads") {
    shouldSubscribe()
    shouldHaveQuote(Quote.empty(EUR.currency))
    gateway.relayMessage(Order(Bid, BtcAmount(1), EUR(900)), PeerConnection("client1"))
    shouldHaveQuote(Quote(EUR.currency, Some(EUR(900)) -> None))
    gateway.relayMessage(Order(Ask, BtcAmount(0.8), EUR(950)), PeerConnection("client2"))
    shouldHaveQuote(Quote(EUR.currency, Some(EUR(900)) -> Some(EUR(950))))
  }

  it must "quote last price" in new WithEurBroker("quote-last-price") {
    shouldSubscribe()
    gateway.relayMessage(Order(Bid, BtcAmount(1), EUR(900)), PeerConnection("client1"))
    gateway.relayMessage(Order(Ask, BtcAmount(1), EUR(800)), PeerConnection("client2"))
    shouldSpawnArbiter()
    shouldHaveQuote(Quote(EUR.currency, lastPrice = Some(EUR(850))))
  }

  it must "reject orders in other currencies" in new WithEurBroker("reject-other-currencies") {
    shouldSubscribe()
    EventFilter.error(pattern = ".*", occurrences = 1) intercept {
      broker ! ReceiveMessage(Order(Bid, BtcAmount(1), USD(900)), PeerConnection("client"))
      gateway.expectNoMsg()
    }
  }

  it must "cancel orders" in new WithEurBroker("cancel-orders") {
    shouldSubscribe()
    gateway.relayMessage(Order(Bid, BtcAmount(1), EUR(900)), PeerConnection("client1"))
    gateway.relayMessage(Order(Ask, BtcAmount(0.8), EUR(950)), PeerConnection("client2"))
    gateway.relayMessage(CancelOrder(EUR.currency), PeerConnection("client1"))
    shouldHaveQuote(Quote(EUR.currency, None -> Some(EUR(950))))
  }

  it must "expire old orders" in new WithEurBroker("expire-orders") {
    shouldSubscribe()
    gateway.relayMessage(Order(Bid, BtcAmount(1), EUR(900)), PeerConnection("client"))
    gateway.expectNoMsg()
    shouldHaveQuote(Quote.empty(EUR.currency))
  }

  it must "keep priority of orders when resubmitted" in new WithEurBroker("keep-priority") {
    shouldSubscribe()
    gateway.relayMessage(Order(Bid, BtcAmount(1), EUR(900)), PeerConnection("first-bid"))
    gateway.relayMessage(Order(Bid, BtcAmount(1), EUR(900)), PeerConnection("second-bid"))
    gateway.relayMessage(Order(Bid, BtcAmount(1), EUR(900)), PeerConnection("first-bid"))
    gateway.relayMessage(Order(Ask, BtcAmount(1), EUR(900)), PeerConnection("ask"))
    val orderMatch = shouldSpawnArbiter()
    orderMatch.buyer should equal (PeerConnection("first-bid"))
  }

  it must "label crosses with random identifiers" in new WithEurBroker("random-id") {
    shouldSubscribe()
    gateway.relayMessage(Order(Bid, BtcAmount(1), EUR(900)), PeerConnection("buyer"))
    gateway.relayMessage(Order(Ask, BtcAmount(1), EUR(900)), PeerConnection("seller"))
    val id1 = shouldSpawnArbiter().exchangeId
    gateway.relayMessage(Order(Bid, BtcAmount(1), EUR(900)), PeerConnection("buyer"))
    gateway.relayMessage(Order(Ask, BtcAmount(1), EUR(900)), PeerConnection("seller"))
    val id2 = shouldSpawnArbiter().exchangeId
    id1 should not (equal (id2))
  }
}
