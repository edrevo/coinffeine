package com.coinffeine.client

import com.coinffeine.common.PeerConnection
import com.coinffeine.common.Currency.Implicits._
import com.coinffeine.common.bitcoin.{KeyPair, PublicKey}
import com.coinffeine.common.network.CoinffeineUnitTestNetwork
import com.coinffeine.common.protocol.messages.exchange.ExchangeId

trait SampleExchangeInfo extends CoinffeineUnitTestNetwork.Component {

  val exchangeId = ExchangeId("id")

  val sampleExchangeInfo = ExchangeInfo(
    exchangeId,
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
