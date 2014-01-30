package com.bitwise.bitmarket.client.handshake

import scala.util.Try

import akka.actor.{ActorRef, Props}
import com.google.bitcoin.core.Transaction
import com.google.bitcoin.crypto.TransactionSignature

import com.bitwise.bitmarket.common.PeerConnection

/** A handshake actor is in charge of entering into a value exchange by getting a refundSignature
  * transaction signed and relying on the broker to publish the commitment TX.
  */
object HandshakeActor {

  /** Handshake configuration */
  trait ExchangeHandshake {
    val id: String
    val counterpart: PeerConnection
    val broker: PeerConnection
    val commitmentTransaction: Transaction
    val refundTransaction: Transaction

    /** Signs counterpart refundSignature transaction
      *
      * @param refundTransaction  Transaction to sign
      * @return  TX signature or a failure if the refundSignature was not valid (e.g. incorrect
      *          amount)
      */
    def signCounterpartRefundTransaction(refundTransaction: Transaction): Try[TransactionSignature]

    /** Validate counterpart signature of our refundSignature transaction.
      *
      * @param signature  Signed refundSignature TX
      * @return           Success when valid or rejection cause as an exception
      */
    def validateRefundSignature(signature: TransactionSignature): Try[Unit]
  }

  /** Sent to the handshake listeners to notify success with a refundSignature transaction or
    * failure with an exception.
    */
  case class HandshakeResult(refundSig: Try[TransactionSignature])

  case class RefundSignatureTimeoutException(exchangeId: String) extends RuntimeException(
    s"Timeout waiting for a valid signature of the refund transaction of handshake $exchangeId")

  trait Component {
    /** Create the properties of a handshake actor.
      *
      * @param exchangeHandshake  Handshake to perform
      * @param messageGateway     Communications gateway
      * @param blockchain         Actor to ask for TX confirmations for
      * @param resultListeners    Actors to be notified of the handshake result
      * @return                   Actor properties
      */
    def handshakeActorProps(
        exchangeHandshake: ExchangeHandshake,
        messageGateway: ActorRef,
        blockchain: ActorRef,
        resultListeners: Seq[ActorRef]): Props
  }
}
