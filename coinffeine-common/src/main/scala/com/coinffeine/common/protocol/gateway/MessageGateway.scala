package com.coinffeine.common.protocol.gateway

import akka.actor.Props
import com.googlecode.protobuf.pro.duplex.PeerInfo

import com.coinffeine.common.PeerConnection
import com.coinffeine.common.protocol.messages.PublicMessage

object MessageGateway {

  /** A message sent in order to forward another message to a given destination. */
  case class ForwardMessage[M <: PublicMessage](message: M, dest: PeerConnection)

  type Filter = ReceiveMessage[_ <: PublicMessage] => Boolean

  /** A message sent in order to subscribe for incoming messages.
    *
    * Each actor can only have one active subscription at a time. A second Subscribe message
    * sent to the gateway would overwrite any previous subscription.
    *
    * @param filter A filter function that indicates what messages are forwarded to the sender actor
    */
  case class Subscribe(filter: Filter)

  /** A message sent in order to unsubscribe from incoming message reception. */
  case object Unsubscribe

  /** A message send back to the subscriber. */
  case class ReceiveMessage[M <: PublicMessage](msg: M, sender: PeerConnection)

  /** An exception thrown when an error is found on message forward. */
  case class ForwardException(message: String, cause: Throwable = null)
    extends RuntimeException(message, cause)

  trait Component {
    def messageGatewayProps(serverInfo: PeerInfo): Props
  }
}
