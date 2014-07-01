package com.coinffeine.client

import com.coinffeine.common.PeerConnection
import com.coinffeine.common.Currency.Implicits._
import com.coinffeine.common.bitcoin.{KeyPair, PublicKey}
import com.coinffeine.common.exchange.{Both, Exchange}
import com.coinffeine.common.network.CoinffeineUnitTestNetwork

trait SampleExchangeInfo extends CoinffeineUnitTestNetwork.Component {

  val broker = PeerConnection("broker")

  val exchange = Exchange(
    id = Exchange.Id("id"),
    amounts = Exchange.Amounts(
      bitcoinAmount = 10.BTC,
      fiatAmount = 10.EUR,
      breakdown = Exchange.StepBreakdown(intermediateSteps = 10)
    ),
    parameters = Exchange.Parameters(lockTime = 25, network),
    connections = Both(buyer = PeerConnection("buyer"), seller = PeerConnection("seller")),
    participants = Both(
      buyer = Exchange.PeerInfo(
        paymentProcessorAccount = "buyer",
        bitcoinKey = new PublicKey()
      ),
      seller = Exchange.PeerInfo(
        paymentProcessorAccount = "seller",
        bitcoinKey = new KeyPair()
      )
    ),
    broker = Exchange.BrokerInfo(broker)
  )
}
