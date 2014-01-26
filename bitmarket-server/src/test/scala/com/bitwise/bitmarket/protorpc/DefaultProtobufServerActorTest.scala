package com.bitwise.bitmarket.protorpc

import scala.concurrent._
import scala.concurrent.duration._
import scala.language.postfixOps

import akka.actor.Props
import akka.testkit.TestProbe
import com.googlecode.protobuf.pro.duplex.PeerInfo
import org.scalatest.concurrent.{IntegrationPatience, Eventually}

import com.bitwise.bitmarket.AkkaSpec
import com.bitwise.bitmarket.broker.BrokerActor._
import com.bitwise.bitmarket.broker.TestClient
import com.bitwise.bitmarket.common.currency.BtcAmount
import com.bitwise.bitmarket.common.currency.CurrencyCode.{EUR, USD}
import com.bitwise.bitmarket.common.protocol._
import com.bitwise.bitmarket.common.protocol.protobuf.{BitmarketProtobuf => msg}
import com.bitwise.bitmarket.common.protocol.protobuf.ProtobufConversions.toProtobuf

class DefaultProtobufServerActorTest
  extends AkkaSpec("ProtobufServerSystem") with Eventually with IntegrationPatience {

  val testTimeout = 3 seconds
  val basePort = 8000
  val eurBroker = TestProbe()
  val serverInfo = new PeerInfo("localhost", basePort)
  val server = system.actorOf(Props(new DefaultProtobufServerActor(
    serverInfo,
    brokers = Map(EUR.currency -> eurBroker.ref),
    brokerTimeout = 1 second
  )))
  val clients = List.tabulate(5)(index => new TestClient(basePort + index + 1, serverInfo))

  override def afterAll() {
    super.afterAll()
    clients.foreach(_.shutdown())
  }

  "A protobuf server actor" should "translate and forward order placements" in {
    val bid = Bid(BtcAmount(0.7), EUR(650), clients(0).connection.toString)
    clients(0).connectToServer()
    clients(0).placeOrder(toProtobuf(bid)).getResult should be (msg.OrderResponse.Result.SUCCESS)
    eurBroker.expectMsg(OrderPlacement(bid))
  }

  it should "reject order placements for non traded currencies" in {
    val bid = Bid(BtcAmount(0.7), USD(650), "bitmarket://localhost:8001/")
    clients(1).connectToServer()
    clients(1).placeOrder(toProtobuf(bid)).getResult should
      be (msg.OrderResponse.Result.CURRENCY_NOT_TRADED)
    eurBroker.expectNoMsg()
  }

  it should "translate quote requests and forward broker response" in {
    val currentQuote = Quote(EUR(10) -> EUR(20), EUR(15))
    clients(2).connectToServer()
    val quote = clients(2).requestQuote(EUR.currency)
    eurBroker.expectMsg(QuoteRequest)
    eurBroker.reply(QuoteResponse(currentQuote))
    Await.result(quote, testTimeout).getQuote should be (toProtobuf(currentQuote))
  }

  it should "translate quote requests and timeout if broker stop responding"  in {
    clients(3).connectToServer()
    val quote = clients(3).requestQuote(EUR.currency)
    eurBroker.expectMsg(QuoteRequest)
    Await.result(quote, testTimeout).getResult should be (msg.QuoteResponse.Result.TIMEOUT)
  }

  it should "notify both parts with order matches on book crosses" in {
    val orderMatch = OrderMatch(
      id = "1",
      amount = BtcAmount(1),
      price = EUR(870),
      buyer = clients(0).connection.toString,
      seller = clients(1).connection.toString
    )
    eurBroker.send(server, NotifyCross(orderMatch))
    eventually {
      clients(0).receivedMessages contains toProtobuf(orderMatch)
      clients(1).receivedMessages contains toProtobuf(orderMatch)
    }
  }
}
