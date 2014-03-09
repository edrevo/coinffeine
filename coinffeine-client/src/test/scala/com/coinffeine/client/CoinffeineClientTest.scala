package com.coinffeine.client

import akka.testkit.TestProbe

import com.coinffeine.common.{PeerConnection, AkkaSpec}
import com.coinffeine.common.protocol.gateway.MessageGateway.{ForwardMessage, ReceiveMessage}
import com.coinffeine.common.protocol.messages.MessageSend

abstract class CoinffeineClientTest(systemName: String) extends AkkaSpec(systemName) {
  val gateway = TestProbe()
  val counterpart: PeerConnection
  val broker: PeerConnection

  def fromCounterpart(message: Any) = ReceiveMessage(message, counterpart)

  def fromBroker(message: Any) = ReceiveMessage(message, broker)

  def shouldForwardToCounterpart[T : MessageSend](message: T): Unit =
    gateway.expectMsg(ForwardMessage(message, counterpart))

  def shouldForwardToBroker[T : MessageSend](message: T): Unit =
    gateway.expectMsg(ForwardMessage(message, broker))
}
