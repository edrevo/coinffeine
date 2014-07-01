package com.coinffeine.common.exchange.impl

import com.coinffeine.common.PeerConnection
import com.coinffeine.common.Currency.Implicits._
import com.coinffeine.common.bitcoin.KeyPair
import com.coinffeine.common.exchange.{Both, Exchange}
import com.coinffeine.common.network.CoinffeineUnitTestNetwork

object Samples {
  val exchange = Exchange(
    id = Exchange.Id("id"),
    parameters = Exchange.Parameters(
      bitcoinAmount = 1.BTC,
      fiatAmount = 1000.EUR,
      breakdown = Exchange.StepBreakdown(10),
      lockTime = 10,
      network = CoinffeineUnitTestNetwork
    ),
    participants = Both(
      buyer = Exchange.PeerInfo(PeerConnection("buyer"), "buyerAccount", new KeyPair()),
      seller = Exchange.PeerInfo(PeerConnection("seller"), "sellerAccount", new KeyPair())
    ),
    broker = Exchange.BrokerInfo(PeerConnection("broker"))
  )
}
