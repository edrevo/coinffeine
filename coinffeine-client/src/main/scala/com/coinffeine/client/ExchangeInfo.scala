package com.coinffeine.client

import com.coinffeine.common.FiatCurrency
import com.coinffeine.common.exchange.{Exchange, Role}

case class ExchangeInfo[C <: FiatCurrency](role: Role, exchange: Exchange[C]) {
  val id = exchange.id
  val parameters = exchange.parameters
  val broker = exchange.broker
  val user = role.me(exchange)
  val counterpart = role.her(exchange)

  require(user.bitcoinKey.getPrivKeyBytes != null, "Credentials do not contain private key")
  val fiatStepAmount = parameters.fiatAmount / parameters.breakdown.intermediateSteps
  val btcStepAmount = parameters.bitcoinAmount / parameters.breakdown.intermediateSteps
}
