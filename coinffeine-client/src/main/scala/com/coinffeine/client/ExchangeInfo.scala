package com.coinffeine.client

import com.coinffeine.common.{FiatCurrency, PeerConnection}
import com.coinffeine.common.bitcoin.{KeyPair, PublicKey}
import com.coinffeine.common.exchange.Exchange
import com.coinffeine.common.protocol.messages.exchange.ExchangeId

case class ExchangeInfo[C <: FiatCurrency](
    id: ExchangeId,
    parameters: Exchange.Parameters[C],
    counterpart: PeerConnection,
    broker: Exchange.BrokerInfo,
    userKey: KeyPair,
    userFiatAddress: String,
    counterpartKey: PublicKey,
    counterpartFiatAddress: String) {
  require(userKey.getPrivKeyBytes != null, "Credentials do not contain private key")
  val fiatStepAmount = parameters.fiatAmount / parameters.breakdown.intermediateSteps
  val btcStepAmount = parameters.bitcoinAmount / parameters.breakdown.intermediateSteps
}
