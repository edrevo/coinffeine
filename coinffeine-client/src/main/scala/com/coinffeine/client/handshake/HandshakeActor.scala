package com.coinffeine.client.handshake

import scala.util.Try

import akka.actor.{ActorRef, Props}
import com.google.bitcoin.core.Sha256Hash
import com.google.bitcoin.crypto.TransactionSignature

/** A handshake actor is in charge of entering into a value exchange by getting a refundSignature
  * transaction signed and relying on the broker to publish the commitment TX.
  */
object HandshakeActor {
  /** Sent to the handshake listeners to notify success with a refundSignature transaction or
    * failure with an exception.
    */
  case class HandshakeResult(refundSig: Try[TransactionSignature])

  case class RefundSignatureTimeoutException(exchangeId: String) extends RuntimeException(
    s"Timeout waiting for a valid signature of the refund transaction of handshake $exchangeId")

  case class CommitmentTransactionRejectedException(
       exchangeId: String, rejectedTx: Sha256Hash, isOwn: Boolean) extends RuntimeException(
    s"Commitment transaction $rejectedTx (${if (isOwn) "ours" else "counterpart"}) was rejected"
  )

  case class HandshakeAbortedException(exchangeId: String, reason: String) extends RuntimeException(
    s"Handshake $exchangeId aborted externally: $reason"
  )

  trait Component {
    /** Create the properties of a handshake actor.
      *
      * @param handshake        Class that contains the logic to perform the handshake
      * @param messageGateway           Communications gateway
      * @param blockchain               Actor to ask for TX confirmations for
      * @param resultListeners          Actors to be notified of the handshake result
      * @return                         Actor properties
      */
    def handshakeActorProps(
        handshake: Handshake,
        messageGateway: ActorRef,
        blockchain: ActorRef,
        resultListeners: Seq[ActorRef]): Props
  }
}
