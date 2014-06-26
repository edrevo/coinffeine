package com.coinffeine.client

import scala.concurrent.Future

import akka.actor.{ActorContext, ActorRef}
import akka.dispatch.ExecutionContexts
import akka.pattern.pipe

import com.coinffeine.common.{FiatCurrency, PeerConnection}
import com.coinffeine.common.exchange.{Exchange, Role}
import com.coinffeine.common.protocol.gateway.MessageGateway.ForwardMessage
import com.coinffeine.common.protocol.messages.PublicMessage

class MessageForwarding(messageGateway: ActorRef,
                        counterpart: PeerConnection,
                        broker: PeerConnection) {

  def this(messageGateway: ActorRef, exchange: Exchange[_ <: FiatCurrency], role: Role) =
    this(messageGateway, role.her(exchange).connection, exchange.broker.connection)

  def forwardToCounterpart(message: PublicMessage): Unit =
    forwardMessage(message, counterpart)

  def forwardToCounterpart(message: Future[PublicMessage])
                                    (implicit context: ActorContext): Unit =
    forwardMessage(message, counterpart)

  def forwardToBroker(message: PublicMessage): Unit =
    forwardMessage(message, broker)

  def forwardToBroker(message: Future[PublicMessage])
                               (implicit context: ActorContext): Unit =
    forwardMessage(message, broker)

  def forwardMessage(message: PublicMessage, address: PeerConnection): Unit =
    messageGateway ! ForwardMessage(message, address)

  def forwardMessage(message: Future[PublicMessage], address: PeerConnection)
                    (implicit context: ActorContext): Unit = {
    implicit val executionContext = ExecutionContexts.global()
    message.map(ForwardMessage(_, address)).pipeTo(messageGateway)(context.self)
  }
}
