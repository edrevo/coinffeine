package com.coinffeine.server

import java.net.BindException

import akka.actor.{ActorRef, Props}
import akka.testkit.TestProbe
import com.googlecode.protobuf.pro.duplex.PeerInfo

import com.coinffeine.broker.BrokerActor.BrokeringStart
import com.coinffeine.common.{AkkaSpec, MockActor}
import com.coinffeine.common.MockActor._
import com.coinffeine.common.currency.CurrencyCode.EUR
import com.coinffeine.common.protocol.gateway.MessageGateway.{Bind, BoundTo}
import com.coinffeine.common.protocol.messages.brokerage.Market
import com.coinffeine.common.system.ActorSystemBootstrap
import com.coinffeine.server.BrokerSupervisorActor.InitializedBroker

class BrokerSupervisorActorTest extends AkkaSpec {

  val gatewayProbe = TestProbe()
  val gatewayProps = MockActor.props(gatewayProbe)
  val brokerProbe = TestProbe()
  val brokerProps = MockActor.props(brokerProbe)

  val server = system.actorOf(Props(
    new BrokerSupervisorActor(Set(EUR.currency), gatewayProps, brokerProps)))
  var brokerRef: ActorRef = _
  var gatewayRef: ActorRef = _

  "The server actor" should "wait for initialization" in {
    gatewayProbe.expectNoMsg()
    server ! ActorSystemBootstrap.Start(Array("--port", "8080"))
  }

  it should "initialize gateway" in {
    gatewayRef = gatewayProbe.expectMsgClass(classOf[MockStarted]).ref
    gatewayProbe.expectMsgPF() {
      case MockReceived(_, _, Bind(serverInfo)) if serverInfo.getPort == 8080 =>
    }
    gatewayRef ! MockSend(server, BoundTo(new PeerInfo("localhost", 8080)))
  }

  it should "initialize brokers" in {
    brokerRef = brokerProbe.expectMsgClass(classOf[MockStarted]).ref
    brokerProbe.expectMsgPF() {
      case MockReceived(_, _, BrokeringStart(Market(EUR.currency), _)) =>
    }
  }

  it should "notify successful initialization" in {
    expectMsg(InitializedBroker)
  }

  it should "restart brokers" in {
    brokerRef ! MockThrow(new Error("Something went wrong"))
    brokerProbe.expectMsgClass(classOf[MockStopped])
    brokerProbe.expectMsgClass(classOf[MockRestarted])
  }

  it should "stop if protobuf server crashes" in {
    gatewayRef ! MockThrow(new BindException("Something went wrong"))
    gatewayProbe.expectMsgClass(classOf[MockStopped])
    gatewayProbe.expectNoMsg()
  }
}
