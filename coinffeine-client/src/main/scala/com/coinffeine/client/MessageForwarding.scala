package com.coinffeine.client

import scala.concurrent.Future

import akka.actor.{ActorContext, ActorRef}
import akka.dispatch.ExecutionContexts
import akka.pattern.pipe

import com.coinffeine.common.PeerConnection
import com.coinffeine.common.protocol.gateway.MessageGateway.ForwardMessage
import com.coinffeine.common.protocol.messages.PublicMessage

trait MessageForwarding {
  protected def exchangeInfo: ExchangeInfo
  protected def messageGateway: ActorRef

  protected def forwardToCounterpart(message: PublicMessage): Unit =
    forwardMessage(message, exchangeInfo.counterpart)

  protected def forwardToCounterpart(message: Future[PublicMessage])
                                    (implicit context: ActorContext): Unit =
    forwardMessage(message, exchangeInfo.counterpart)

  protected def forwardToBroker(message: PublicMessage): Unit =
    forwardMessage(message, exchangeInfo.broker)

  protected def forwardToBroker(message: Future[PublicMessage])
                               (implicit context: ActorContext): Unit =
    forwardMessage(message, exchangeInfo.broker)

  protected def forwardMessage(message: PublicMessage, address: PeerConnection): Unit =
    messageGateway ! ForwardMessage(message, address)

  protected def forwardMessage(message: Future[PublicMessage], address: PeerConnection)
                              (implicit context: ActorContext): Unit = {
    implicit val executionContext = ExecutionContexts.global()
    message.map(ForwardMessage(_, address)).pipeTo(messageGateway)(context.self)
  }
}
