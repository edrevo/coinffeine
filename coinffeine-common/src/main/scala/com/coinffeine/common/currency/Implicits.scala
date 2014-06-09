package com.coinffeine.common.currency

object Implicits {
  import scala.language.implicitConversions

  class CurrencyImplicits(i: BigDecimal) {
    def EUR: FiatAmount = CurrencyCode.EUR(i)
    def USD: FiatAmount = CurrencyCode.USD(i)
  }
  implicit def pimpMyDouble(i: Double) = new CurrencyImplicits(i)
  implicit def pimpMyDecimal(i: BigDecimal) = new CurrencyImplicits(i)
  implicit def pimpMyInt(i: Int) = new CurrencyImplicits(BigDecimal(i))
}
