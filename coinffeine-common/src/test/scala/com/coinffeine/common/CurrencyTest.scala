package com.coinffeine.common

import java.util.{Currency => JavaCurrency}

import org.scalatest.{ShouldMatchers, FlatSpec}

class CurrencyTest extends FlatSpec with ShouldMatchers {

  "US Dollar" must behave like validFiatCurrency(Currency.UsDollar, "USD")
  "Euro" must behave like validFiatCurrency(Currency.Euro, "EUR")
  "Bitcoin" must behave like validCurrency(Currency.Bitcoin)

  private def validCurrency(currency: Currency): Unit =  {

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

  private def validFiatCurrency(currency: FiatCurrency, currencyCode: String): Unit = {

    validCurrency(currency)

    it must "return the corresponding Java currency instance" in {
      currency.javaCurrency should be(JavaCurrency.getInstance(currencyCode))
    }
  }
}
