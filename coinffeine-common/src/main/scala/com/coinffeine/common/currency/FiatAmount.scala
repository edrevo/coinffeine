package com.coinffeine.common.currency

import java.util.Currency
import scala.math.Ordering

case class FiatAmount(amount: BigDecimal, currency: Currency) {

  /** Alternative constructor for Java code */
  def this(amount: java.math.BigDecimal, currency: Currency) =
    this(new BigDecimal(amount), currency)

  def +(that: FiatAmount): FiatAmount = {
    FiatAmount.requireCompatible(this, that)
    FiatAmount(this.amount + that.amount, this.currency)
  }

  def -(that: FiatAmount): FiatAmount = {
    FiatAmount.requireCompatible(this, that)
    FiatAmount(this.amount - that.amount, this.currency)
  }

  def unary_- : FiatAmount = FiatAmount(-amount, currency)

  def / (divisor: BigDecimal): FiatAmount = copy(amount = amount / divisor)

  override def toString = "%s %s".format(amount.underlying().toPlainString, currency)
}

object FiatAmount extends Ordering[FiatAmount] {
  override def compare(x: FiatAmount, y: FiatAmount): Int = {
    requireCompatible(x, y)
    x.amount.compare(y.amount)
  }

  private def requireCompatible(x: FiatAmount, y: FiatAmount) {
    require(x.currency == y.currency)
  }
}
