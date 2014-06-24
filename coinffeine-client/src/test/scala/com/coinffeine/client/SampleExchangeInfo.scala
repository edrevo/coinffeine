package com.coinffeine.client

import com.coinffeine.common.PeerConnection
import com.coinffeine.common.Currency.Implicits._
import com.coinffeine.common.bitcoin.{KeyPair, PublicKey}
import com.coinffeine.common.exchange.Exchange
import com.coinffeine.common.network.CoinffeineUnitTestNetwork
import com.coinffeine.common.protocol.messages.exchange.ExchangeId

trait SampleExchangeInfo extends CoinffeineUnitTestNetwork.Component {

  val exchangeId = ExchangeId("id")

  val sampleExchangeInfo = ExchangeInfo(
    exchangeId,
    parameters = Exchange.Parameters(
      bitcoinAmount = 10.BTC,
      fiatAmount = 10.EUR,
      breakdown = Exchange.StepBreakdown(intermediateSteps = 10),
      lockTime = 25,
      network
    ),
    counterpart = PeerConnection("counterpart"),
    broker = Exchange.BrokerInfo(PeerConnection("broker")),
    userKey = new KeyPair(),
    userFiatAddress = "",
    counterpartKey = new PublicKey(),
    counterpartFiatAddress = "")
}
