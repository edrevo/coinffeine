package com.coinffeine.broker

import scala.concurrent.duration._

import akka.actor._
import akka.testkit._

import com.coinffeine.broker.BrokerActor.BrokeringStart
import com.coinffeine.common._
import com.coinffeine.common.Currency.{UsDollar, Euro}
import com.coinffeine.common.Currency.Implicits._
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
    val market = Market(Euro)
    val arbiterProbe = TestProbe()
    val gateway = new GatewayProbe()
    val broker = system.actorOf(Props(new BrokerActor(
      handshakeArbiterProps = Props(new FakeHandshakeArbiterActor(arbiterProbe.ref)),
      orderExpirationInterval = 1 second
    )), name)

    def shouldHaveQuote(expectedQuote: Quote[FiatCurrency]): Unit = {
      val quoteRequester = PeerConnection("quoteRequester")
      gateway.relayMessage(QuoteRequest(Euro), quoteRequester)
      gateway.expectForwarding(expectedQuote, quoteRequester)
    }

    def shouldSubscribe(): Subscribe = {
      broker ! BrokeringStart(market, gateway.ref)
      gateway.expectSubscription()
    }

    def shouldSpawnArbiter() = arbiterProbe.expectMsgClass(classOf[OrderMatch])

    def relayBid[C <: FiatCurrency](
        amount: BitcoinAmount, price: CurrencyAmount[C], requester: String) = gateway.relayMessage(
      OrderSet.empty(market).addOrder(Bid, amount, price), PeerConnection(requester))

    def relayAsk[C <: FiatCurrency](
        amount: BitcoinAmount, price: CurrencyAmount[C], requester: String) = gateway.relayMessage(
      OrderSet.empty(market).addOrder(Ask, amount, price), PeerConnection(requester))
  }

  "A broker" must "subscribe himself to relevant messages" in new WithEurBroker("subscribe") {
    val Subscribe(filter) = shouldSubscribe()
    val client: PeerConnection = PeerConnection("client1")
    val relevantOrders = OrderSet.empty(market)
    val irrelevantOrders = OrderSet.empty(Market(UsDollar))
    filter(ReceiveMessage(relevantOrders, PeerConnection("client1"))) should be (true)
    filter(ReceiveMessage(irrelevantOrders, PeerConnection("client1"))) should be (false)
    filter(ReceiveMessage(QuoteRequest(Euro), client)) should be (true)
    filter(ReceiveMessage(QuoteRequest(UsDollar), client)) should be (false)
  }

  it must "keep orders and notify both parts and start an arbiter when they cross" in
    new WithEurBroker("notify-crosses") {
      shouldSubscribe()
      relayBid(1.BTC, 900.EUR, "client1")
      relayBid(0.8.BTC, 950.EUR, "client2")
      relayAsk(0.6.BTC, 850.EUR, "client3")
      val notifiedOrderMatch = arbiterProbe.expectMsgClass(classOf[OrderMatch])
      notifiedOrderMatch.amount should be (0.6.BTC.toBtcAmount)
      notifiedOrderMatch.price should be (900.EUR.toFiatAmount)
      notifiedOrderMatch.buyer should be (PeerConnection("client2"))
      notifiedOrderMatch.seller should be (PeerConnection("client3"))
    }

  it must "quote spreads" in new WithEurBroker("quote-spreads") {
    shouldSubscribe()
    shouldHaveQuote(Quote.empty(Euro))
    relayBid(1.BTC, 900.EUR, "client1")
    shouldHaveQuote(Quote(Euro, Some(900.EUR) -> None))
    relayAsk(0.8.BTC, 950.EUR, "client2")
    shouldHaveQuote(Quote(Euro, Some(900.EUR) -> Some(950.EUR)))
  }

  it must "quote last price" in new WithEurBroker("quote-last-price") {
    shouldSubscribe()
    relayBid(1.BTC, 900.EUR, "client1")
    relayAsk(1.BTC, 800.EUR, "client2")
    shouldSpawnArbiter()
    shouldHaveQuote(Quote(Euro, lastPrice = Some(850.EUR)))
  }

  it must "cancel orders" in new WithEurBroker("cancel-orders") {
    shouldSubscribe()
    relayBid(1.BTC, 900.EUR, "client1")
    relayAsk(0.8.BTC, 950.EUR, "client2")
    gateway.relayMessage(OrderSet.empty(market), PeerConnection("client1"))
    shouldHaveQuote(Quote(Euro, None -> Some(950.EUR)))
  }

  it must "expire old orders" in new WithEurBroker("expire-orders") {
    shouldSubscribe()
    relayBid(1.BTC, 900.EUR, "client")
    gateway.expectNoMsg()
    shouldHaveQuote(Quote.empty(Euro))
  }

  it must "keep priority of orders when resubmitted" in new WithEurBroker("keep-priority") {
    shouldSubscribe()
    relayBid(1.BTC, 900.EUR, "first-bid")
    relayBid(1.BTC, 900.EUR, "second-bid")
    relayBid(1.BTC, 900.EUR, "first-bid")
    relayAsk(1.BTC, 900.EUR, "ask")
    val orderMatch = shouldSpawnArbiter()
    orderMatch.buyer should equal (PeerConnection("first-bid"))
  }

  it must "label crosses with random identifiers" in new WithEurBroker("random-id") {
    shouldSubscribe()
    relayBid(1.BTC, 900.EUR, "buyer")
    relayAsk(1.BTC, 900.EUR, "seller")
    val id1 = shouldSpawnArbiter().exchangeId
    relayBid(1.BTC, 900.EUR, "buyer")
    relayAsk(1.BTC, 900.EUR, "seller")
    val id2 = shouldSpawnArbiter().exchangeId
    id1 should not (equal (id2))
  }
}
