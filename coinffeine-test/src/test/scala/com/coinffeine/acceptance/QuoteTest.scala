package com.coinffeine.acceptance

import org.scalatest.time.{Seconds, Span}

import com.coinffeine.client.api.CoinffeineNetwork.Connected
import com.coinffeine.common.currency.BtcAmount
import com.coinffeine.common.currency.CurrencyCode.EUR
import com.coinffeine.common.paymentprocessor.okpay.OKPayProcessor
import com.coinffeine.common.protocol.messages.brokerage.Quote

class QuoteTest extends AcceptanceTest {

  val timeout = Span(3, Seconds)

  feature("Any peer should be able to query price quotes") {

    scenario("no previous order placed") { f =>
      f.withPeer { peer =>
        Given("that peer is connected but there is no orders placed")
        peer.network.connect().futureValue should be (Connected)

        When("a peer asks for the current quote on a currency")
        val quote = peer.network.currentQuote(OKPayProcessor.Id, EUR.currency)

        Then("he should get an empty quote")
        quote.futureValue should be(Quote.empty(EUR.currency))
      }
    }

    scenario("previous bidding and betting") { f =>
      f.withPeerPair { (bob, sam) =>
        Given("Bob and Sam are connected peers")
        bob.network.connect().futureValue should be (Connected)
        sam.network.connect().futureValue should be (Connected)

        Given("that Bob has placed a bid and Sam an ask that does not cross")
        bob.network.submitBuyOrder(BtcAmount(0.1), EUR(50))
        sam.network.submitSellOrder(BtcAmount(0.3), EUR(180))

        When("Bob asks for the current quote on a currency")
        def quote = bob.network.currentQuote(OKPayProcessor.Id, EUR.currency)

        Then("he should get the current spread")
        eventually {
          quote.futureValue should be(Quote(EUR.currency, Some(EUR(50)) -> Some(EUR(180))))
        }
      }
    }
  }
}
