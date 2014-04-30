package com.coinffeine.acceptance

import org.scalatest.concurrent.ScalaFutures
import org.scalatest.time.{Seconds, Span}

import com.coinffeine.common.currency.BtcAmount
import com.coinffeine.common.currency.CurrencyCode.EUR
import com.coinffeine.common.paymentprocessor.okpay.OKPayProcessor
import com.coinffeine.common.protocol.messages.brokerage.Quote

class QuoteTest extends AcceptanceTest with ScalaFutures {

  val timeout = Span(3, Seconds)

  feature("Any peer should be able to query price quotes") {

    scenario("no previous order placed") { f =>
      f.withPeer { peer =>
        Given("that no peer has placed any order")

        When("a peer asks for the current quote on a currency")
        val quote = peer.network.currentQuote(OKPayProcessor.Id, EUR.currency)

        Then("he should get an empty quote")
        quote.futureValue should be(Quote.empty(EUR.currency))
      }
    }

    ignore("previous bidding and betting") { f =>
      f.withPeerPair { (bob, sam) =>
        Given("that Bob has placed a bid and Sam an ask that does not cross")
        bob.network.submitBuyOrder(BtcAmount(0.1), OKPayProcessor.Id, EUR(50)) isReadyWithin timeout
        sam.network.submitSellOrder(BtcAmount(0.3), OKPayProcessor.Id, EUR(180)) isReadyWithin timeout

        When("Bob asks for the current quote on a currency")
        val quote = bob.network.currentQuote(OKPayProcessor.Id, EUR.currency)

        Then("he should get the current spread")
        quote.futureValue should be(Quote(EUR.currency, Some(EUR(500)) -> Some(EUR(600))))
      }
    }
  }
}
