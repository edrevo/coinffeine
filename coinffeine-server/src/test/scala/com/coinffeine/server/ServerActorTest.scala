package com.coinffeine.server

import java.net.BindException

import akka.actor.Props
import akka.testkit.TestProbe

import com.coinffeine.common.{AkkaSpec, MockActor}
import com.coinffeine.common.MockActor._

class ServerActorTest extends AkkaSpec {

  val gatewayProbe = TestProbe()
  val gatewayProps = MockActor.props(gatewayProbe)
  val brokerProbe = TestProbe()
  val brokerProps = Seq(MockActor.props(brokerProbe))

  val server = system.actorOf(Props(new ServerActor(gatewayProps, _ => brokerProps)))
  val MockStarted(brokerRef) = brokerProbe.expectMsgClass(classOf[MockStarted])
  val MockStarted(gatewayRef) = gatewayProbe.expectMsgClass(classOf[MockStarted])

  "The server actor" must "restart brokers" in {
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
