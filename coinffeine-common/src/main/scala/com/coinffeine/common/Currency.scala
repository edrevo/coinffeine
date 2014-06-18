package com.coinffeine.common

import java.math.BigInteger
import java.util.{Currency => JavaCurrency}
import scala.util.Try

/** An finite amount of currency C.
  *
  * This trait is used to grant polymorphism to currency amounts. You may combine it with a type parameter in any
  * function in order to accept generic currency amounts, as in:
  * {{{
  *   def myFunction[C <: Currency](amount: CurrencyAmount[C]): Unit { ... }
  * }}}
  *
  * @tparam C The type of currency this amount is represented in
  */
case class CurrencyAmount[+C <: Currency](
    value: BigDecimal, currency: C) extends PartiallyOrdered[CurrencyAmount[C]] {

  require(currency.isValidAmount(value),
    "Tried to create a currency amount which is invalid for that currency: " + this.toString)

  def +[B >: C <: Currency] (other: CurrencyAmount[B]): CurrencyAmount[B] =
    copy(value = value + other.value)
  def -[B >: C <: Currency] (other: CurrencyAmount[B]): CurrencyAmount[B] =
    copy(value = value - other.value)
  def * (mult: BigDecimal): CurrencyAmount[C] = copy(value = value * mult)
  def / (divisor: BigDecimal): CurrencyAmount[C] = copy(value = value / divisor)
  def unary_- : CurrencyAmount[C] = copy(value = -value)

  def min[B >: C <: Currency](that: CurrencyAmount[B]): CurrencyAmount[B] =
    if (this.value <= that.value) this else that
  def max[B >: C <: Currency](that: CurrencyAmount[B]): CurrencyAmount[B] =
    if (this.value >= that.value) this else that

  val isPositive = value > 0
  val isNegative = value < 0

  override def tryCompareTo[B >: CurrencyAmount[C] <% PartiallyOrdered[B]](that: B): Option[Int] =
    Try {
      val thatAmount = that.asInstanceOf[CurrencyAmount[_ <: FiatCurrency]]
      require(thatAmount.currency == this.currency)
      thatAmount
    }.toOption.map(thatAmount => this.value.compare(thatAmount.value))

  override def toString = value.toString() + " " + currency.toString
}

/** Representation of a currency. */
trait Currency {

  /** The instance of the concrete type that extends this trait. */
  val self: this.type = this

  /** An amount of currency.
    *
    * Please note this is a path-dependent type. It cannot be used as Currency.Amount but as UsDollar.Amount,
    * Euros.Amount, etc. If you want to use currency value in a polymorphic way, please use CurrencyAmount.
    *
    * @param value The value represented by this amount.
    */
  def amount(value: BigDecimal): CurrencyAmount[this.type] = CurrencyAmount(value, self)

  def isValidAmount(value: BigDecimal): Boolean

  def apply(value: BigDecimal) = amount(value)
  def apply(value: Int) = amount(value)
  def apply(value: Double) = amount(value)
  def apply(value: java.math.BigDecimal) = amount(BigDecimal(value))

  def toString: String

  lazy val Zero: CurrencyAmount[this.type] = apply(0)
}

/** A fiat currency. */
trait FiatCurrency extends Currency {
  val javaCurrency: JavaCurrency
  override lazy val toString = javaCurrency.getCurrencyCode
}

object FiatCurrency {

  def apply(javaCurrency: JavaCurrency): FiatCurrency = javaCurrency match {
    case Currency.UsDollar.javaCurrency => Currency.UsDollar
    case Currency.Euro.javaCurrency => Currency.Euro
    case _ => throw new IllegalArgumentException(s"cannot convert $javaCurrency into a known Coinffeine fiat currency")
  }
}

object Currency {

  object UsDollar extends FiatCurrency {
    val javaCurrency = JavaCurrency.getInstance("USD")
    override def isValidAmount(amount: BigDecimal) = (amount * 100).isWhole()
  }

  object Euro extends FiatCurrency {
    val javaCurrency = JavaCurrency.getInstance("EUR")
    override def isValidAmount(amount: BigDecimal) = (amount * 100).isWhole()
  }

  object Bitcoin extends Currency {
    val OneBtcInSatoshi = BigDecimal(100000000)
    override val toString = "BTC"

    def fromSatoshi(amount: BigInteger) = Bitcoin(BigDecimal(amount) / OneBtcInSatoshi)

    override def isValidAmount(amount: BigDecimal) =
      (amount * OneBtcInSatoshi).isWhole()
  }

  object Implicits {
    import scala.language.implicitConversions

    implicit class BitcoinSatoshiConverter(btc: BitcoinAmount) {

      def asSatoshi = (btc.value * Bitcoin.OneBtcInSatoshi).toBigIntExact().get.underlying()

    }

    class UnitImplicits(i: BigDecimal) {
      def BTC: BitcoinAmount = Bitcoin(i)

      def EUR: CurrencyAmount[Euro.type] = Euro(i)

      def USD: CurrencyAmount[UsDollar.type] = UsDollar(i)
    }

    implicit def pimpMyDouble(i: Double) = new UnitImplicits(i)

    implicit def pimpMyDecimal(i: BigDecimal) = new UnitImplicits(i)

    implicit def pimpMyInt(i: Int) = new UnitImplicits(BigDecimal(i))
  }
}

