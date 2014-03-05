package com.coinffeine.common.protocol.messages.brokerage

import org.scalatest.FlatSpec
import org.scalatest.matchers.MustMatchers

import com.coinffeine.common.currency.CurrencyCode.{EUR, USD}

class QuoteTest extends FlatSpec with MustMatchers {

  "A quote" must "use the same currency for all its prices" in {
    val ex = evaluating {
      Quote(EUR(10) -> USD(20), EUR(15))
    } must produce [IllegalArgumentException]
    ex.toString must include ("All prices should use the same currency")
  }

  it must "print to a readable string" in {
    Quote(EUR(10) -> EUR(20), EUR(15)).toString must
      be ("Quote(spread = (10 EUR, 20 EUR), last = 15 EUR)")
    Quote(Some(EUR(10)) -> None, None).toString must be ("Quote(spread = (10 EUR, --), last = --)")
  }
}
