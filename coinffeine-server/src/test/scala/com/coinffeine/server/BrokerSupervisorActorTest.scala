package com.coinffeine.server

import java.net.BindException

import akka.actor.Props
import akka.testkit.TestProbe
import com.googlecode.protobuf.pro.duplex.PeerInfo

import com.coinffeine.common.{AkkaSpec, MockActor}
import com.coinffeine.common.MockActor._
import com.coinffeine.common.protocol.gateway.MessageGateway.Bind

class BrokerSupervisorActorTest extends AkkaSpec {

  val gatewayProbe = TestProbe()
  val gatewayProps = MockActor.props(gatewayProbe)
  val brokerProbe = TestProbe()
  val brokerProps = Seq(MockActor.props(brokerProbe))
  val serverInfo = new PeerInfo("localhost", 8080)

  val server = system.actorOf(Props(
    new BrokerSupervisorActor(serverInfo, gatewayProps, _ => brokerProps)))
  val MockStarted(brokerRef) = brokerProbe.expectMsgClass(classOf[MockStarted])
  val MockStarted(gatewayRef) = gatewayProbe.expectMsgClass(classOf[MockStarted])

  "The server actor" must "initialize services" in {
    gatewayProbe.expectMsgPF() {
      case MockReceived(_, _, Bind(`serverInfo`)) =>
    }
  }

  it must "restart brokers" in {
    brokerRef ! MockThrow(new Error("Something went wrong"))
    brokerProbe.expectMsgClass(classOf[MockStopped])
    brokerProbe.expectMsgClass(classOf[MockRestarted])
  }

  it must "stop if protobuf server crashes" in {
    gatewayRef ! MockThrow(new BindException("Something went wrong"))
    gatewayProbe.expectMsgClass(classOf[MockStopped])
    gatewayProbe.expectNoMsg()
  }
}
