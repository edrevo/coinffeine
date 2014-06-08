package com.coinffeine.common

package object protocol {
  type Spread[C <: FiatCurrency] = (Option[CurrencyAmount[C]], Option[CurrencyAmount[C]])
}
