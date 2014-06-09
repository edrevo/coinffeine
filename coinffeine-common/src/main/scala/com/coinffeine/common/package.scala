package com.coinffeine

import com.coinffeine.common.Currency.Bitcoin

package object common {

  type BitcoinAmount = CurrencyAmount[Bitcoin.type]
  type FiatAmount = CurrencyAmount[FiatCurrency]
}
