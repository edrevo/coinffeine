package com.coinffeine.common.currency

import java.util.Currency

class CurrencyCode(symbol: String) {

  val currency: Currency = Currency.getInstance(symbol)

  def apply(amount: BigDecimal): FiatAmount = FiatAmount(amount, currency)
}

object CurrencyCode {
  val EUR = new CurrencyCode("EUR")
  val USD = new CurrencyCode("USD")
}
