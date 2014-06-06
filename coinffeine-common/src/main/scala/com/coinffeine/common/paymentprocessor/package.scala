package com.coinffeine.common

package object paymentprocessor {

  type AnyPayment = Payment[_ <: FiatCurrency]
}
