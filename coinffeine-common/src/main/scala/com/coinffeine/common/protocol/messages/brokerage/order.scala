package com.coinffeine.common.protocol.messages.brokerage

import com.coinffeine.common.currency.{FiatAmount => LegacyFiatAmount}
import com.coinffeine.common.{FiatCurrency, CurrencyAmount, BitcoinAmount, FiatAmount}

sealed trait OrderType {
  def priceOrdering[C <: FiatCurrency]: Ordering[CurrencyAmount[C]]
  @deprecated def legacyPriceOrdering: Ordering[LegacyFiatAmount]
}

/** Trying to buy bitcoins */
case object Bid extends OrderType {
  override def priceOrdering[C <: FiatCurrency] = Ordering.by[CurrencyAmount[C], BigDecimal](x => -x.value)
  @deprecated override def legacyPriceOrdering: Ordering[LegacyFiatAmount] =
    Ordering.by[LegacyFiatAmount, BigDecimal](x => -x.amount)
}

/** Trying to sell bitcoins */
case object Ask extends OrderType {
  override def priceOrdering[C <: FiatCurrency] = Ordering.by[CurrencyAmount[C], BigDecimal](_.value)
  @deprecated override def legacyPriceOrdering: Ordering[LegacyFiatAmount] =
    LegacyFiatAmount
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
