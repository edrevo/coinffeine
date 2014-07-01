package com.coinffeine.common

package object exchange {

  /** An exchange for any currency */
  type AnyExchange = Exchange[_ <: FiatCurrency]
}
