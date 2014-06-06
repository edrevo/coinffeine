package com.coinffeine.common

import java.util.{Currency => JavaCurrency}
import java.math.BigInteger

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
trait CurrencyAmount[C <: Currency] {

  val currency: C
  val value: BigDecimal

  def + (other: CurrencyAmount[C]): CurrencyAmount[C]
  def - (other: CurrencyAmount[C]): CurrencyAmount[C]
  def * (mult: BigDecimal): CurrencyAmount[C]
  def / (divisor: BigDecimal): CurrencyAmount[C]
  def unary_- : CurrencyAmount[C]

  val isPositive = value > 0
  val isNegative = value < 0
}

/** Representation of a currency. */
trait Currency {

  /** The type of the concrete type that extends this trait, using a curiously recurring template pattern. */
  type SelfType <: Currency

  /** The instance of the concrete type that extends this trait. */
  val self: SelfType

  /** An amount of currency.
    *
    * Please note this is a path-dependent type. It cannot be used as Currency.Amount but as UsDollar.Amount,
    * Euros.Amount, etc. If you want to use currency value in a polymorphic way, please use CurrencyAmount.
    *
    * @param value The value represented by this amount.
    */
  case class Amount(value: BigDecimal) extends CurrencyAmount[SelfType] {

    def this(value: Int) = this(BigDecimal(value))
    def this(value: Double) = this(BigDecimal(value))
    def this(value: java.math.BigDecimal) = this(BigDecimal(value))

    override val currency: SelfType = self

    override def + (other: CurrencyAmount[SelfType]) = Amount(value + other.value)
    override def - (other: CurrencyAmount[SelfType]) = Amount(value - other.value)
    override def * (mult: BigDecimal) = Amount(value * mult)
    override def / (divisor: BigDecimal) = Amount(value / divisor)
    override def unary_- = Amount(-value)
  }

  object Amount extends Ordering[Amount] {

    val Zero = Amount(0)
    override def compare(x: Amount, y: Amount): Int = x.value.compare(y.value)
  }

  def apply(value: BigDecimal) = Amount(value)
  def apply(value: Int) = Amount(value)
  def apply(value: Double) = Amount(value)
  def apply(value: java.math.BigDecimal) = Amount(BigDecimal(value))
}

/** A fiat currency. */
trait FiatCurrency extends Currency {
  val javaCurrency: JavaCurrency
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
    type SelfType = UsDollar.type
    val self = this
    val javaCurrency = JavaCurrency.getInstance("USD")
  }

  object Euro extends FiatCurrency {
    type SelfType = Euro.type
    val self = this
    val javaCurrency = JavaCurrency.getInstance("EUR")
  }

  object Bitcoin extends Currency {
    type SelfType = Bitcoin.type
    val self = this
    val OneBtcInSatoshi = BigDecimal(100000000)

    def fromSatoshi(amount: BigInteger) = Bitcoin(BigDecimal(amount) / OneBtcInSatoshi)
  }

  object Implicits {
    import scala.language.implicitConversions

    implicit class BitcoinSatoshiConverter(btc: Bitcoin.Amount) {

      def asSatoshi = (btc.value * Bitcoin.OneBtcInSatoshi).toBigIntExact().get.underlying()

    }

    class UnitImplicits(i: BigDecimal) {
      def BTC: Bitcoin.Amount = Bitcoin(i)

      def EUR: Euro.Amount = Euro(i)

      def USD: UsDollar.Amount = UsDollar(i)
    }

    implicit def pimpMyDouble(i: Double) = new UnitImplicits(i)

    implicit def pimpMyDecimal(i: BigDecimal) = new UnitImplicits(i)

    implicit def pimpMyInt(i: Int) = new UnitImplicits(BigDecimal(i))
  }
}

