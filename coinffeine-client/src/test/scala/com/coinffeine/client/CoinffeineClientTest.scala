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

  protected class ValidateWithPeer(validation: PeerConnection => Unit) {
    def to(receiver: PeerConnection): Unit = validation(receiver)
  }

  def shouldForward[T: MessageSend](message: T) =
    new ValidateWithPeer(receiver => gateway.expectMsg(ForwardMessage(message, receiver)))

  protected class ValidateAllMessagesWithPeer {
    private var messages: List[PeerConnection => Any] = List.empty
    def message[T : MessageSend](msg: T): ValidateAllMessagesWithPeer = {
      messages = ((receiver: PeerConnection) => ForwardMessage(msg, receiver)) :: messages
      this
    }
    def to(receiver: PeerConnection): Unit = {
      gateway.expectMsgAllOf(messages.map(_(receiver)): _*)
    }
  }

  def shouldForwardAll = new ValidateAllMessagesWithPeer
}
