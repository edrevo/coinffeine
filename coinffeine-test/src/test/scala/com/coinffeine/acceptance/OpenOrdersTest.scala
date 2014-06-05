package com.coinffeine.acceptance

import org.scalatest.time.{Seconds, Span}

import scala.concurrent.duration._

import com.coinffeine.client.api.CoinffeineNetwork.Connected
import com.coinffeine.common.currency.CurrencyCode.EUR
import com.coinffeine.common.currency.Implicits._
import com.coinffeine.common.protocol.ProtocolConstants

class OpenOrdersTest extends AcceptanceTest {

  val timeout = Span(3, Seconds)

  override val protocolConstants = ProtocolConstants.DefaultConstants.copy(
    orderExpirationInterval = 2.seconds
  )

  feature("A peer should manage its orders") {

    scenario("orders are resent while not matched") { f =>
      f.withPeer { peer =>
        Given("the peer is connected and have some orders opened")
        peer.network.connect().futureValue should be (Connected)
        peer.network.submitSellOrder(0.1.BTC, 100.EUR)

        When("more than order timeout time has passed")
        Thread.sleep((peer.protocolConstants.orderExpirationInterval * 2).toMillis)

        Then("orders are have not been discarded")
        peer.network.currentQuote(EUR.currency).futureValue.spread should be (None -> Some(100.EUR))
      }
    }
  }
}
