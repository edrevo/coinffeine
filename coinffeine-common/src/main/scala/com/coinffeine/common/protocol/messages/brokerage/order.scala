package com.coinffeine.common.protocol.messages.brokerage

import com.coinffeine.common.currency.{BtcAmount, FiatAmount}

sealed trait OrderType {
  def priceOrdering: Ordering[FiatAmount]
}

/** Trying to buy bitcoins */
case object Bid extends OrderType {
  override def priceOrdering = Ordering.by[FiatAmount, BigDecimal](x => -x.amount)
}

/** Trying to sell bitcoins */
case object Ask extends OrderType {
  override def priceOrdering = FiatAmount
}

/** Request for an interchange. */
case class Order(orderType: OrderType, amount: BtcAmount, price: FiatAmount) {
  require(amount.amount > 0, "Amount ordered must be strictly positive")
  require(price.amount > 0, "Price must be strictly positive")
}

object Order {
  implicit val naturalOrdering: Ordering[Order] = Ordering.by[Order, BigDecimal] {
    case Order(Bid, _, price) => -price.amount
    case Order(Ask, _, price) => price.amount
  }
}
