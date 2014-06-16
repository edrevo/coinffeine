package com.coinffeine.client

import akka.testkit.{ImplicitSender, TestProbe}

import com.coinffeine.common.{AkkaSpec, PeerConnection}
import com.coinffeine.common.protocol.gateway.MessageGateway.{ForwardMessage, ReceiveMessage}
import com.coinffeine.common.protocol.messages.PublicMessage

abstract class CoinffeineClientTest(systemName: String)
  extends AkkaSpec(systemName) with SampleExchangeInfo with ImplicitSender {

  val gateway = TestProbe()
  val counterpart: PeerConnection
  val broker: PeerConnection

  def fromCounterpart(message: PublicMessage) = ReceiveMessage(message, counterpart)

  def fromBroker(message: PublicMessage) = ReceiveMessage(message, broker)

  protected class ValidateWithPeer(validation: PeerConnection => Unit) {
    def to(receiver: PeerConnection): Unit = validation(receiver)
  }

  def shouldForward(message: PublicMessage) =
    new ValidateWithPeer(receiver => gateway.expectMsg(ForwardMessage(message, receiver)))

  protected class ValidateAllMessagesWithPeer {
    private var messages: List[PeerConnection => Any] = List.empty
    def message(msg: PublicMessage): ValidateAllMessagesWithPeer = {
      messages = ((receiver: PeerConnection) => ForwardMessage(msg, receiver)) :: messages
      this
    }
    def to(receiver: PeerConnection): Unit = {
      gateway.expectMsgAllOf(messages.map(_(receiver)): _*)
    }
  }

  def shouldForwardAll = new ValidateAllMessagesWithPeer
}
