package com.coinffeine.client

import com.google.bitcoin.core.ECKey

import com.coinffeine.common.PeerConnection
import com.coinffeine.common.Currency.Implicits._
import com.coinffeine.common.network.UnitTestNetworkComponent

trait SampleExchangeInfo extends UnitTestNetworkComponent {
  val sampleExchangeInfo = ExchangeInfo(
    "id",
    PeerConnection("counterpart"),
    PeerConnection("broker"),
    network,
    userKey = new ECKey(),
    userFiatAddress = "",
    counterpartKey = new ECKey(),
    counterpartFiatAddress = "",
    btcExchangeAmount = 10 BTC,
    fiatExchangeAmount = 10 EUR,
    steps = 10,
    lockTime = 25)
}
