package com.coinffeine.common.exchange.impl

import com.coinffeine.common.PeerConnection
import com.coinffeine.common.Currency.Implicits._
import com.coinffeine.common.bitcoin.KeyPair
import com.coinffeine.common.exchange.{CompleteExchange, Both, Exchange}
import com.coinffeine.common.network.CoinffeineUnitTestNetwork

object Samples {
  val exchange = CompleteExchange(
    id = Exchange.Id("id"),
    amounts = Exchange.Amounts(
      bitcoinAmount = 1.BTC,
      fiatAmount = 1000.EUR,
      breakdown = Exchange.StepBreakdown(10)
    ),
    parameters = Exchange.Parameters(lockTime = 10, CoinffeineUnitTestNetwork),
    connections = Both(buyer = PeerConnection("buyer"), seller = PeerConnection("seller")),
    participants = Both(
      buyer = Exchange.PeerInfo("buyerAccount", new KeyPair()),
      seller = Exchange.PeerInfo("sellerAccount", new KeyPair())
    ),
    broker = Exchange.BrokerInfo(PeerConnection("broker"))
  )
}
