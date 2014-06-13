package com.coinffeine.client.exchange

import akka.actor.ActorRef

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
}
