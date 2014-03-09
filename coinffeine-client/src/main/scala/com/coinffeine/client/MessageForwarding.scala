package com.coinffeine.client

import scala.concurrent.Future

import akka.actor.{Actor, ActorRef}
import akka.dispatch.ExecutionContexts
import akka.pattern.pipe

import com.coinffeine.common.PeerConnection
import com.coinffeine.common.protocol.gateway.MessageGateway.ForwardMessage
import com.coinffeine.common.protocol.messages.MessageSend

trait MessageForwarding {
  this: Actor =>

  protected val exchange: Exchange
  protected val messageGateway: ActorRef

  protected def forwardToCounterpart[T : MessageSend](message: T) {
    forwardMessage(message, exchange.counterpart)
  }

  protected def forwardToCounterpart[T : MessageSend](message: Future[T]) {
    forwardMessage(message, exchange.counterpart)
  }

  protected def forwardToBroker[T : MessageSend](message: T) {
    forwardMessage(message, exchange.broker)
  }

  protected def forwardToBroker[T : MessageSend](message: Future[T]) {
    forwardMessage(message, exchange.broker)
  }

  protected def forwardMessage[T : MessageSend](message: T, address: PeerConnection) {
    messageGateway ! ForwardMessage(message, address)
  }

  protected def forwardMessage[T : MessageSend](message: Future[T], address: PeerConnection) {
    implicit val executionContext = ExecutionContexts.global()
    message.map(ForwardMessage(_, address)).pipeTo(messageGateway)(self)
  }
}
