package com.coinffeine.acceptance

import org.scalatest.time.{Seconds, Span}

import scala.concurrent.duration._

import com.coinffeine.client.api.CoinffeineNetwork.Connected
import com.coinffeine.common.currency.CurrencyCode.EUR
import com.coinffeine.common.Currency.Implicits._
import com.coinffeine.common.protocol.ProtocolConstants
import com.coinffeine.common.Currency.Euro

class OpenOrdersTest extends AcceptanceTest {

  val timeout = Span(3, Seconds)

  override val protocolConstants = ProtocolConstants.DefaultConstants.copy(
    orderExpirationInterval = 2.seconds,
    orderResubmitInterval = 1.second
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
        peer.network.currentQuote(Euro).futureValue.spread should be (None -> Some(100.EUR))
      }
    }

    scenario("orders get cancelled") { f =>
      f.withPeer { peer =>
        Given("the peer is connected and have some orders opened")
        peer.network.connect().futureValue should be(Connected)
        peer.network.submitSellOrder(0.1.BTC, 100.EUR)
        peer.network.submitSellOrder(0.1.BTC, 120.EUR)

        When("some order is cancelled")
        peer.network.cancelSellOrder(0.1.BTC, 120.EUR)

        Then("the order gets removed from the order book")
        eventually {
          peer.network.currentQuote(Euro).futureValue.spread should be (None -> Some(100.EUR))
        }
      }
    }
  }
}
