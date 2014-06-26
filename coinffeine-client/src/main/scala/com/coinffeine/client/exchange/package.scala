package com.coinffeine.client

import com.coinffeine.common.FiatCurrency

package object exchange {

  type AnyExchange = ProtoMicroPaymentChannel[_ <: FiatCurrency]
}
