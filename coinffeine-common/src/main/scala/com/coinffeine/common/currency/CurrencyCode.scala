package com.coinffeine.common.currency

import java.util.Currency

@deprecated
class CurrencyCode(symbol: String) {

  val currency: Currency = Currency.getInstance(symbol)

  def apply(amount: BigDecimal): FiatAmount = FiatAmount(amount, currency)
}

object CurrencyCode {
  @deprecated val EUR = new CurrencyCode("EUR")
  @deprecated val USD = new CurrencyCode("USD")
}
