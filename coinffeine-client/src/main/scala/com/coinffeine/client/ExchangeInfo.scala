package com.coinffeine.client

import com.coinffeine.common.{BitcoinAmount, CurrencyAmount, FiatCurrency, PeerConnection}
import com.coinffeine.common.bitcoin.{KeyPair, Network, PublicKey}
import com.coinffeine.common.protocol.messages.exchange.ExchangeId

case class ExchangeInfo[C <: FiatCurrency](
    id: ExchangeId,
    counterpart: PeerConnection,
    broker: PeerConnection,
    network: Network,
    userKey: KeyPair,
    userFiatAddress: String,
    counterpartKey: PublicKey,
    counterpartFiatAddress: String,
    btcExchangeAmount: BitcoinAmount,
    fiatExchangeAmount: CurrencyAmount[C],
    steps: Int,
    lockTime: Long) {
  require(steps > 0, "Steps must be greater than zero")
  require(btcExchangeAmount.isPositive, "Exchange amount must be greater than zero")
  require(fiatExchangeAmount.isPositive)
  require(userKey.getPrivKeyBytes != null, "Credentials do not contain private key")
  val fiatStepAmount = fiatExchangeAmount / steps
  val btcStepAmount = btcExchangeAmount / steps
}
