package com.coinffeine.client

import com.coinffeine.common.FiatCurrency
import com.coinffeine.common.exchange.Exchange
import com.coinffeine.common.protocol.messages.exchange.ExchangeId

case class ExchangeInfo[C <: FiatCurrency](
    id: ExchangeId,
    parameters: Exchange.Parameters[C],
    user: Exchange.PeerInfo,
    counterpart: Exchange.PeerInfo,
    broker: Exchange.BrokerInfo) {
  require(user.bitcoinKey.getPrivKeyBytes != null, "Credentials do not contain private key")
  val fiatStepAmount = parameters.fiatAmount / parameters.breakdown.intermediateSteps
  val btcStepAmount = parameters.bitcoinAmount / parameters.breakdown.intermediateSteps
}
