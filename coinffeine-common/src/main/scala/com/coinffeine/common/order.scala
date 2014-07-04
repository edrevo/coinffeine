package com.coinffeine.common

sealed trait OrderType {
  def name: String
  def priceOrdering[C <: FiatCurrency]: Ordering[CurrencyAmount[C]]

  override def toString = name
}

/** Trying to buy bitcoins */
case object Bid extends OrderType {
  override val name = "Bid (buy)"
  override def priceOrdering[C <: FiatCurrency] = Ordering.by[CurrencyAmount[C], BigDecimal](x => -x.value)
}

/** Trying to sell bitcoins */
case object Ask extends OrderType {
  override val name = "Ask (sell)"
  override def priceOrdering[C <: FiatCurrency] = Ordering.by[CurrencyAmount[C], BigDecimal](_.value)
}

/** Request for an interchange. */
case class Order(orderType: OrderType, amount: BitcoinAmount, price: FiatAmount) {
  require(amount.isPositive, "Amount ordered must be strictly positive")
  require(price.isPositive, "Price must be strictly positive")
}

object Order {
  implicit val naturalOrdering: Ordering[Order] = Ordering.by[Order, BigDecimal] {
    case Order(Bid, _, price) => -price.value
    case Order(Ask, _, price) => price.value
  }
}
