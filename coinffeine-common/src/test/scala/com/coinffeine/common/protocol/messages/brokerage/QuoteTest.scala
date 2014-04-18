package com.coinffeine.common.protocol.messages.brokerage

import com.coinffeine.common.UnitTest
import com.coinffeine.common.currency.CurrencyCode.{EUR, USD}

class QuoteTest extends UnitTest {

  "A quote" must "use the same currency for all its prices" in {
    the [IllegalArgumentException] thrownBy  {
      Quote(EUR(10) -> USD(20), EUR(15))
    } should have message "requirement failed: Inconsistent price 20 USD, EUR was expected"
  }

  it must "print to a readable string" in {
    Quote(EUR(10) -> EUR(20), EUR(15)).toString should
      be ("Quote(spread = (10 EUR, 20 EUR), last = 15 EUR)")
    Quote(EUR.currency, Some(EUR(10)) -> None, None).toString should
      be ("Quote(spread = (10 EUR, --), last = --)")
  }
}
