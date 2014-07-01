package com.coinffeine.common

package object exchange {

  /** An exchange for any currency */
  type AnyExchange = Exchange[_ <: FiatCurrency]

  /** An ongoing exchange for any currency */
  type AnyOngoingExchange = OngoingExchange[_ <: FiatCurrency]
}
