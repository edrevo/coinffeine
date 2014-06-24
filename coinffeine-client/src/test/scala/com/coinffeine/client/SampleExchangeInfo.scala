package com.coinffeine.client

import com.coinffeine.common.Currency.Euro
import com.coinffeine.common.PeerConnection
import com.coinffeine.common.Currency.Implicits._
import com.coinffeine.common.bitcoin.{KeyPair, PublicKey}
import com.coinffeine.common.exchange.{BuyerRole, Exchange, SellerRole}
import com.coinffeine.common.network.CoinffeineUnitTestNetwork
import com.coinffeine.common.protocol.messages.exchange.ExchangeId

trait SampleExchangeInfo extends CoinffeineUnitTestNetwork.Component {

  val exchangeId = ExchangeId("id")

  val sellerExchangeInfo: ExchangeInfo[Euro.type] = ExchangeInfo(
    SellerRole,
    Exchange(
      exchangeId,
      parameters = Exchange.Parameters(
        bitcoinAmount = 10.BTC,
        fiatAmount = 10.EUR,
        breakdown = Exchange.StepBreakdown(intermediateSteps = 10),
        lockTime = 25,
        network
      ),
      buyer = Exchange.PeerInfo(
        connection = PeerConnection("buyer"),
        paymentProcessorAccount = "buyer",
        bitcoinKey = new PublicKey()
      ),
      seller = Exchange.PeerInfo(
        connection = PeerConnection("seller"),
        paymentProcessorAccount = "seller",
        bitcoinKey = new KeyPair()
      ),
      broker = Exchange.BrokerInfo(PeerConnection("broker"))
    )
  )
  val buyerExchangeInfo: ExchangeInfo[Euro.type] = sellerExchangeInfo.copy(role = BuyerRole)
  val sampleExchangeInfo = sellerExchangeInfo
}
