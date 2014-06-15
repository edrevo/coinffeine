package com.coinffeine.client.exchange

import akka.actor.ActorRef
import com.google.bitcoin.core.Transaction
import com.google.bitcoin.crypto.TransactionSignature

import com.coinffeine.common.protocol.ProtocolConstants
import com.coinffeine.common.FiatCurrency

/** An exchange actor is in charge of performing each of the exchange steps by sending/receiving
  * bitcoins and fiat.
  */
object ExchangeActor {

  /** Sent to the the actor to start the actual exchange. */
  case class StartExchange[C <: FiatCurrency, Role <: UserRole](
      exchange: Exchange[C] with Role,
      constants: ProtocolConstants,
      messageGateway: ActorRef,
      resultListeners: Set[ActorRef]
  )

  /** Sent to the exchange listeners to notify success of the exchange */
  case object ExchangeSuccess

  /** Sent to the exchange listeners to notify of a failure during the exchange */
  case class ExchangeFailure(cause: Throwable, lastOffer: Option[Transaction])

  private[exchange] case object StepSignatureTimeout

  case class TimeoutException(message: String) extends RuntimeException(message)

  case class InvalidStepSignature(
      step: Int,
      sig0: TransactionSignature,
      sig1: TransactionSignature,
      cause: Throwable) extends RuntimeException(
    s"Received an invalid step signature for step $step. Received: ($sig0, $sig1)",
    cause)
}
