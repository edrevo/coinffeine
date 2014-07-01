package com.coinffeine.common.exchange

import com.coinffeine.common.FiatCurrency

/** A value class with all needed details for an exchange after a successful handshake */
case class OngoingExchange[+C <: FiatCurrency](role: Role, underlying: Exchange[C]) {
  val id = underlying.id
  val parameters = underlying.parameters
  val connections = underlying.connections
  val participants = underlying.participants
  val broker = underlying.broker
  val amounts = underlying.amounts
  val requiredSignatures = underlying.requiredSignatures
}
