package com.coinffeine.client

import com.coinffeine.common.PeerConnection
import com.coinffeine.common.Currency.Implicits._
import com.coinffeine.common.bitcoin.{KeyPair, PublicKey}
import com.coinffeine.common.network.UnitTestNetworkComponent

trait SampleExchangeInfo extends UnitTestNetworkComponent {
  val sampleExchangeInfo = ExchangeInfo(
    "id",
    PeerConnection("counterpart"),
    PeerConnection("broker"),
    network,
    userKey = new KeyPair(),
    userFiatAddress = "",
    counterpartKey = new PublicKey(),
    counterpartFiatAddress = "",
    btcExchangeAmount = 10 BTC,
    fiatExchangeAmount = 10 EUR,
    steps = 10,
    lockTime = 25)
}
