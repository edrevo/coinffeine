package com.coinffeine.client

import scala.concurrent.Future

import akka.actor.{Actor, ActorRef}
import akka.dispatch.ExecutionContexts
import akka.pattern.pipe

import com.coinffeine.common.PeerConnection
import com.coinffeine.common.protocol.gateway.MessageGateway.ForwardMessage
import com.coinffeine.common.protocol.messages.PublicMessage

trait MessageForwarding {
  this: Actor =>

  protected val exchangeInfo: ExchangeInfo
  protected val messageGateway: ActorRef

  protected def forwardToCounterpart(message: PublicMessage): Unit =
    forwardMessage(message, exchangeInfo.counterpart)

  protected def forwardToCounterpart(message: Future[PublicMessage]): Unit =
    forwardMessage(message, exchangeInfo.counterpart)

  protected def forwardToBroker(message: PublicMessage): Unit =
    forwardMessage(message, exchangeInfo.broker)

  protected def forwardToBroker(message: Future[PublicMessage]): Unit =
    forwardMessage(message, exchangeInfo.broker)

  protected def forwardMessage(message: PublicMessage, address: PeerConnection): Unit =
    messageGateway ! ForwardMessage(message, address)

  protected def forwardMessage(message: Future[PublicMessage], address: PeerConnection): Unit = {
    implicit val executionContext = ExecutionContexts.global()
    message.map(ForwardMessage(_, address)).pipeTo(messageGateway)(self)
  }
}
