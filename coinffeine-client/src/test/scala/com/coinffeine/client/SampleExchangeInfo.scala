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
    parameters = Exchange.Parameters(
      bitcoinAmount = 10.BTC,
      fiatAmount = 10.EUR,
      breakdown = Exchange.StepBreakdown(intermediateSteps = 10),
      lockTime = 25,
      network
    ),
    participants = Both(
      buyer = Exchange.PeerInfo(
        connection = PeerConnection("buyer"),
        paymentProcessorAccount = "buyer",
        bitcoinKey = new PublicKey()
      ),
      seller = Exchange.PeerInfo(
        connection = PeerConnection("seller"),
        paymentProcessorAccount = "seller",
        bitcoinKey = new KeyPair()
      )
    ),
    broker = Exchange.BrokerInfo(broker)
  )
}
