package com.coinffeine.client

import com.google.bitcoin.core.ECKey

import com.coinffeine.common.{Currency, PeerConnection}
import com.coinffeine.common.currency.Implicits._
import com.coinffeine.common.network.UnitTestNetworkComponent

trait WithSampleExchangeInfo extends UnitTestNetworkComponent {
  val sampleExchangeInfo = ExchangeInfo(
    "id",
    PeerConnection("counterpart"),
    PeerConnection("broker"),
    network,
    userKey = new ECKey(),
    userFiatAddress = "",
    counterpartKey = new ECKey(),
    counterpartFiatAddress = "",
    btcExchangeAmount = Currency.Bitcoin(10),
    fiatExchangeAmount = Currency.Euro(10),
    steps = 10,
    lockTime = 25)
}
