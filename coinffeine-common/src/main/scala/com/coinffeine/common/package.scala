package com.coinffeine

package object common {

  type AnyCurrencyAmount = CurrencyAmount[_ <: Currency]
  type AnyFiatCurrencyAmount = CurrencyAmount[_ <: FiatCurrency]
}
