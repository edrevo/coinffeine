package com.bitwise.bitmarket.common.currency

case class BtcAmount(amount: BigDecimal) extends Ordered[BtcAmount] {

  /** Alternative constructor for Java code */
  def this(amount: java.math.BigDecimal) = this(new BigDecimal(amount))

  def min(other: BtcAmount): BtcAmount = if (this <= other) this else other
  def +(that: BtcAmount) = BtcAmount(amount + that.amount)
  def -(that: BtcAmount) = BtcAmount(amount - that.amount)
  def unary_- = BtcAmount(-amount)

  override def compare(that: BtcAmount): Int = this.amount.compare(that.amount)

  override def toString = amount.underlying().toPlainString + " BTC"
}

object BtcAmount extends Ordering[BtcAmount] {
  override def compare(x: BtcAmount, y: BtcAmount): Int = x.amount.compare(y.amount)
}
