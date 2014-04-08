package com.coinffeine.common.currency


case class BtcAmount(amount: BigDecimal) extends Ordered[BtcAmount] {
  lazy val asSatoshi = (amount * BtcAmount.OneBtcInSatoshi).toBigIntExact().get.underlying()

  def min(other: BtcAmount): BtcAmount = if (this <= other) this else other
  def +(that: BtcAmount) = BtcAmount(amount + that.amount)
  def -(that: BtcAmount) = BtcAmount(amount - that.amount)
  def /(that: Long) = BtcAmount(amount / that)
  def *(that: Int) = BtcAmount(amount * that)
  def unary_- = BtcAmount(-amount)

  override def compare(that: BtcAmount): Int = this.amount.compare(that.amount)

  override def toString = amount.underlying().toPlainString + " BTC"
}

object BtcAmount {
  val OneBtcInSatoshi = BigDecimal(100000000)

  def apply(amount: java.math.BigDecimal): BtcAmount = BtcAmount(new BigDecimal(amount))

  def apply(amount: java.math.BigInteger): BtcAmount =
    BtcAmount(BigDecimal(amount) / OneBtcInSatoshi)
  implicit object BtcAmountNumeric extends Numeric[BtcAmount] {
    def plus(x: BtcAmount, y: BtcAmount): BtcAmount = x + y

    def minus(x: BtcAmount, y: BtcAmount): BtcAmount = x - y

    def times(x: BtcAmount, y: BtcAmount): BtcAmount = BtcAmount(x.amount * y.amount)

    def negate(x: BtcAmount): BtcAmount = BtcAmount(-x.amount)

    def fromInt(x: Int): BtcAmount = BtcAmount(BigInt(x).underlying())

    def toInt(x: BtcAmount): Int = x.asSatoshi.intValue()

    def toLong(x: BtcAmount): Long = x.asSatoshi.longValue()

    def toFloat(x: BtcAmount): Float = x.asSatoshi.floatValue()

    def toDouble(x: BtcAmount): Double = x.asSatoshi.doubleValue()

    def compare(x: BtcAmount, y: BtcAmount): Int = x.amount.compare(y.amount)
  }
}
