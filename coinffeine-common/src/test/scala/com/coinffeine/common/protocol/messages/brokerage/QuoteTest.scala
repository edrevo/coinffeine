package com.coinffeine.common.protocol.messages.brokerage

import com.coinffeine.common.UnitTest
import com.coinffeine.common.Currency.Implicits._
import com.coinffeine.common.Currency.Euro

class QuoteTest extends UnitTest {

  "A quote" must "print to a readable string" in {
    Quote(10.EUR -> 20.EUR, 15 EUR).toString should
      be ("Quote(spread = (10 EUR, 20 EUR), last = 15 EUR)")
    Quote(Euro, Some(10 EUR) -> None, None).toString should
      be ("Quote(spread = (10 EUR, --), last = --)")
  }
}
