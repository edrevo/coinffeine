package com.bitwise.bitmarket.server

import java.net.BindException

import akka.actor.Props
import akka.testkit.TestProbe

import com.bitwise.bitmarket.{MockActor, AkkaSpec}
import com.bitwise.bitmarket.MockActor._
import com.bitwise.bitmarket.common.currency.CurrencyCode.EUR

class ServerActorTest extends AkkaSpec {

  val brokerProbe = TestProbe()
  val brokerProps = Map(EUR.currency -> MockActor.props(brokerProbe))
  val protobufServerProbe = TestProbe()
  val protobufServerProps = MockActor.props(protobufServerProbe)

  val server = system.actorOf(Props(new ServerActor(brokerProps, _ => protobufServerProps)))
  val MockStarted(brokerRef) = brokerProbe.expectMsgClass(classOf[MockStarted])
  val MockStarted(protobufServerRef) = protobufServerProbe.expectMsgClass(classOf[MockStarted])

  "The server actor" must "restart brokers" in {
    brokerRef ! MockThrow(new Error("Something went wrong"))
    brokerProbe.expectMsgClass(classOf[MockStopped])
    brokerProbe.expectMsgClass(classOf[MockRestarted])
  }

  it must "stop if protobuf server crashes" in {
    protobufServerRef ! MockThrow(new BindException("Something went wrong"))
    protobufServerProbe.expectMsgClass(classOf[MockStopped])
    protobufServerProbe.expectNoMsg()
  }
}
