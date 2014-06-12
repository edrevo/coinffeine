package com.coinffeine.common

import java.util.{Currency => JavaCurrency}

import org.scalatest.{ShouldMatchers, FlatSpec}

import com.coinffeine.common.Currency.{Bitcoin, Euro, UsDollar}

class CurrencyTest extends FlatSpec with ShouldMatchers {

  "US Dollar" must behave like validFiatCurrency(UsDollar, "USD")
  it must behave like validCurrencyWithCents(UsDollar)

  "Euro" must behave like validFiatCurrency(Euro, "EUR")
  it must behave like validCurrencyWithCents(Euro)


  "Bitcoin" must behave like validCurrency(Bitcoin)

  it must "detect invalid amounts" in {
    Bitcoin.amount(0.0000001)
    Bitcoin.fromSatoshi(BigInt(1).underlying)
    Bitcoin.amount(0)
    Bitcoin.amount(-100)
    an [IllegalArgumentException] should be thrownBy { Bitcoin.amount(3.1415926535) }
  }

  private def validCurrency(currency: Currency): Unit =  {

    it must "represent amounts in its own currency" in {
      currency.amount(7).currency should be(currency)
    }

    it must "compare amounts of its own currency" in {
      currency.amount(7).tryCompareTo(currency.amount(10)).get should be < 0
      currency.amount(7.5).tryCompareTo(currency.amount(7)).get should be > 0
      currency.amount(2).tryCompareTo(currency.amount(2)).get should be (0)
      object FakeCurrency extends Currency {
        override def isValidAmount(amount: BigDecimal) = true
      }
      currency.amount(3).tryCompareTo(FakeCurrency.amount(4)) should be (None)
    }

    it must "add amounts of its own currency" in {
      currency.amount(2) + currency.amount(3) should be (currency.amount(5))
    }

    it must "subtract amounts of its own currency" in {
      currency.amount(2) - currency.amount(3) should be (currency.amount(-1))
    }

    it must "multiply amounts of its own currency" in {
      currency.amount(2) * BigDecimal(4) should be (currency.amount(8))
    }

    it must "divide amounts of its own currency" in {
      currency.amount(2) / BigDecimal(4) should be (currency.amount(0.5))
    }

    it must "invert amounts of its own currency" in {
      -currency.amount(2) should be (currency.amount(-2))
    }
  }

  private def validFiatCurrency(currency: FiatCurrency, currencyCode: String): Unit = {

    validCurrency(currency)

    it must "return the corresponding Java currency instance" in {
      currency.javaCurrency should be(JavaCurrency.getInstance(currencyCode))
    }
  }

  private def validCurrencyWithCents(currency: FiatCurrency): Unit = {
    it must "detect invalid amounts" in {
      currency.amount(0.01)
      currency.amount(0)
      currency.amount(-3.14)
      a [IllegalArgumentException] should be thrownBy { currency.amount(0.009) }
    }
  }
}
