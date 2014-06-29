package com.coinffeine.common.akka

import akka.actor.Props

import com.coinffeine.common.AkkaSpec
import com.coinffeine.common.akka.ConstantValueActor.{UnsetValue, SetValue}

class ConstantValueActorTest extends AkkaSpec("ConstantValueActorTest") {
  val instance = system.actorOf(Props[ConstantValueActor])

  "A constant value actor" should "start ignoring all non-control messages" in {
    instance ! 5
    expectNoMsg()
  }

  it should "reply to incoming messages after a value has been set" in {
    instance ! SetValue(9)
    instance ! "Hello"
    expectMsg(9)
  }

  it should "stop replyint to incoming messages after the value is unseta " in {
    instance ! UnsetValue
    instance ! "Hello"
    expectNoMsg()
  }
}
