package com.coinffeine.client.exchange

import java.util.concurrent.LinkedBlockingDeque

import akka.testkit.TestActor.Message
import org.scalatest.ShouldMatchers
import org.scalatest.concurrent.Eventually

class TestMessageQueue extends ShouldMatchers with Eventually {
  val queue = new LinkedBlockingDeque[Message]

  def expectMsg[T](msg: T): Unit = {
    waitForMsg()
    queue.pop().msg should be (msg)
  }

  def expectMsgClass[T: Manifest](): Unit = {
    waitForMsg()
    queue.pop().msg shouldBe a [T]
  }

  private def waitForMsg() = eventually { queue should not be ('empty) }
}
