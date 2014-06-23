package com.coinffeine.client.micropayment

import akka.actor.ActorRef

import com.coinffeine.client.exchange.{Exchange, UserRole}
import com.coinffeine.common.FiatCurrency
import com.coinffeine.common.bitcoin.{MutableTransaction, TransactionSignature}
import com.coinffeine.common.protocol.ProtocolConstants

/** A micropayment channel actor is in charge of performing each of the exchange steps by
  * sending/receiving bitcoins and fiat.
  */
object MicroPaymentChannelActor {

  /** Sent to the the actor to start the actual exchange through the micropayment channel. */
  case class StartMicroPaymentChannel[C <: FiatCurrency, Role <: UserRole](
      exchange: Exchange[C] with Role,
      constants: ProtocolConstants,
      messageGateway: ActorRef,
      resultListeners: Set[ActorRef]
  )

  /** Sent to the exchange listeners to notify success of the exchange */
  case object ExchangeSuccess

  /** Sent to the exchange listeners to notify of a failure during the exchange */
  case class ExchangeFailure(cause: Throwable, lastOffer: Option[MutableTransaction])

  private[micropayment] case object StepSignatureTimeout

  case class TimeoutException(message: String) extends RuntimeException(message)

  case class InvalidStepSignature(
      step: Int,
      sig0: TransactionSignature,
      sig1: TransactionSignature,
      cause: Throwable) extends RuntimeException(
    s"Received an invalid step signature for step $step. Received: ($sig0, $sig1)",
    cause)
}
