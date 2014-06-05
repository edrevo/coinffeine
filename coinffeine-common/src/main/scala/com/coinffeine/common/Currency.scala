package com.coinffeine.common

import java.util.{Currency => JavaCurrency}

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

    def + (other: Amount): Amount = Amount(value + other.value)
    def - (other: Amount): Amount = Amount(value - other.value)
    def * (mult: BigDecimal): Amount = Amount(value * mult)
    def / (divisor: BigDecimal): Amount = Amount(value / divisor)
    def unary_- : Amount = Amount(-value)
  }

  object Amount extends Ordering[Amount] {

    override def compare(x: Amount, y: Amount): Int = x.value.compare(y.value)
  }

  /** Convert this currency into a java.util.Currency instance. */
  def toJavaCurrency: Option[JavaCurrency]
}

object Currency {

  object UsDollar extends Currency {
    type SelfType = UsDollar.type
    val self = this
    override def toJavaCurrency = Some(JavaCurrency.getInstance("USD"))
  }

  object Euro extends Currency {
    type SelfType = Euro.type
    val self = this
    override def toJavaCurrency = Some(JavaCurrency.getInstance("EUR"))
  }

  object Bitcoin extends Currency {
    type SelfType = Bitcoin.type
    val self = this
    override def toJavaCurrency = None
  }
}

