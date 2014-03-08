package com.coinffeine.acceptance

import com.coinffeine.common.currency.BtcAmount
import com.coinffeine.common.currency.CurrencyCode.EUR
import com.coinffeine.common.protocol.{Ask, Bid, Order}
import com.coinffeine.common.protocol.messages.brokerage.Quote

class QuoteTest extends AcceptanceTest {

  feature("Any peer should be able to query price quotes") {

    scenario("no previous order placed") { f =>
      f.withPeer { peer =>
        Given("that no peer has placed any order")

        When("a peer asks for the current quote on a currency")
        val quote = peer.askForQuote(EUR.currency)

        Then("he should get an empty quote")
        quote should eventually(be(Quote.empty(EUR.currency)))
      }
    }

    ignore("previous bidding and betting") { f =>
      f.withPeerPair { (bob, sam) =>
        Given("that Bob has placed a bid and Sam an ask that does not cross")
        bob.placeOrder(Order(Bid, BtcAmount(0.1), EUR(500)))
        sam.placeOrder(Order(Ask, BtcAmount(0.3), EUR(600)))

        When("Bob asks for the current quote on a currency")
        val quote = bob.askForQuote(EUR.currency)

        Then("he should get the current spread")
        quote should eventually(be(Quote(EUR.currency, Some(EUR(500)) -> Some(EUR(600)))))
      }
    }
  }
}
