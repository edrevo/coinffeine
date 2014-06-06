package com.coinffeine

import com.coinffeine.common.FiatCurrency

package object client {

  type AnyExchangeInfo = ExchangeInfo[_ <: FiatCurrency]
}
