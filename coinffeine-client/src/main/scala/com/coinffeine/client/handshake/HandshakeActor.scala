package com.coinffeine.client.handshake

import akka.actor.{ActorRef, Props}

import com.coinffeine.common.FiatCurrency
import com.coinffeine.common.bitcoin.{Hash, ImmutableTransaction}
import com.coinffeine.common.exchange.{Exchange, Role}
import com.coinffeine.common.protocol.ProtocolConstants

/** A handshake actor is in charge of entering into a value exchange by getting a refundSignature
  * transaction signed and relying on the broker to publish the commitment TX.
  */
object HandshakeActor {

  /** Sent to the actor to start the handshake
    *
    * @constructor
    * @param exchange         Exchange to start the handshake for
    * @param role             Which role to take
    * @param handshake        Class that contains the logic to perform the handshake
    * @param constants        Protocol constants
    * @param messageGateway   Communications gateway
    * @param blockchain       Actor to ask for TX confirmations for
    * @param resultListeners  Actors to be notified of the handshake result
    */
  case class StartHandshake[C <: FiatCurrency](
      exchange: Exchange[C],
      role: Role,
      @deprecated handshake: Handshake[C],
      constants: ProtocolConstants,
      messageGateway: ActorRef,
      blockchain: ActorRef,
      resultListeners: Set[ActorRef])

  /** Sent to the handshake listeners to notify success with a refundSignature transaction or
    * failure with an exception.
    */
  case class HandshakeSuccess(
    sellerCommitmentTxId: Hash,
    buyerCommitmentTxId: Hash,
    refundTransaction: ImmutableTransaction
  )

  case class HandshakeFailure(e: Throwable)

  case class RefundSignatureTimeoutException(exchangeId: Exchange.Id) extends RuntimeException(
    s"Timeout waiting for a valid signature of the refund transaction of handshake $exchangeId")

  case class CommitmentTransactionRejectedException(
       exchangeId: Exchange.Id, rejectedTx: Hash, isOwn: Boolean) extends RuntimeException(
    s"Commitment transaction $rejectedTx (${if (isOwn) "ours" else "counterpart"}) was rejected"
  )

  case class HandshakeAbortedException(exchangeId: Exchange.Id, reason: String)
    extends RuntimeException(s"Handshake $exchangeId aborted externally: $reason")

  trait Component {
    /** Create the properties of a handshake actor.
      *
      * @return                 Actor properties
      */
    def handshakeActorProps[C <: FiatCurrency]: Props
  }
}
