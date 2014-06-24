package com.coinffeine.common.exchange.impl

import scala.concurrent.duration._

import com.coinffeine.common.PeerConnection
import com.coinffeine.common.Currency.Implicits._
import com.coinffeine.common.bitcoin.KeyPair
import com.coinffeine.common.exchange.Exchange
import com.coinffeine.common.network.CoinffeineUnitTestNetwork
import com.coinffeine.common.protocol.messages.exchange.ExchangeId

object Samples {
  val exchange = Exchange(
    id = ExchangeId("id"),
    parameters = Exchange.Parameters(
      bitcoinAmount = 1.BTC,
      fiatAmount = 1000.EUR,
      breakdown = Exchange.StepBreakdown(10),
      lockTime = 10,
      commitmentConfirmations = 1,
      resubmitRefundSignatureTimeout = 10.seconds,
      refundSignatureAbortTimeout = 30.minutes,
      network = CoinffeineUnitTestNetwork
    ),
    buyer = Exchange.PeerInfo(PeerConnection("buyer"), "buyerAccount", new KeyPair()),
    seller = Exchange.PeerInfo(PeerConnection("seller"), "sellerAccount", new KeyPair()),
    broker = Exchange.BrokerInfo(PeerConnection("broker"))
  )
}
