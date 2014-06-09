package com.coinffeine.common.protocol.messages.brokerage

import com.coinffeine.common.{FiatCurrency, CurrencyAmount, BitcoinAmount, FiatAmount}

sealed trait OrderType {
  def priceOrdering[C <: FiatCurrency]: Ordering[CurrencyAmount[C]]
}

/** Trying to buy bitcoins */
case object Bid extends OrderType {
  override def priceOrdering[C <: FiatCurrency] = Ordering.by[CurrencyAmount[C], BigDecimal](x => -x.value)
}

/** Trying to sell bitcoins */
case object Ask extends OrderType {
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
