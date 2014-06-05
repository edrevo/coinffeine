package com.coinffeine.common

import java.util.{Currency => JavaCurrency}

import org.scalatest.{ShouldMatchers, FlatSpec}

class CurrencyTest extends FlatSpec with ShouldMatchers {

  "US Dollar" must behave like localizedCurrency(Currency.UsDollar, "USD")
  "Euro" must behave like localizedCurrency(Currency.Euro, "EUR")
  "Bitcoin" must behave like representableCurrency(Currency.Bitcoin)

  private def representableCurrency(currency: Currency): Unit =  {

    it must "represent amounts in its own currency" in {
      currency.Amount(7).currency should be(currency)
    }

    it must "compare amounts of its own currency" in {
      currency.Amount.compare(currency.Amount(7), currency.Amount(10)) should be < 0
      currency.Amount.compare(currency.Amount(7.5), currency.Amount(7)) should be > 0
      currency.Amount.compare(currency.Amount(2), currency.Amount(2)) should be (0)
    }

    it must "add amounts of its own currency" in {
      currency.Amount(2) + currency.Amount(3) should be (currency.Amount(5))
    }

    it must "subtract amounts of its own currency" in {
      currency.Amount(2) - currency.Amount(3) should be (currency.Amount(-1))
    }

    it must "multiply amounts of its own currency" in {
      currency.Amount(2) * BigDecimal(4) should be (currency.Amount(8))
    }

    it must "divide amounts of its own currency" in {
      currency.Amount(2) / BigDecimal(4) should be (currency.Amount(0.5))
    }

    it must "invert amounts of its own currency" in {
      -currency.Amount(2) should be (currency.Amount(-2))
    }
  }

  private def localizedCurrency(currency: Currency, currencyCode: String): Unit = {

    representableCurrency(currency)

    it must "return the corresponding Java currency instance" in {
      currency.toJavaCurrency should be(Some(JavaCurrency.getInstance(currencyCode)))
    }
  }
}
