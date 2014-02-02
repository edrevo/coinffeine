package com.bitwise.bitmarket.common.protocol

import com.bitwise.bitmarket.common.currency.{FiatAmount, BtcAmount}

sealed trait OrderType

/** Trying to buy bitcoins */
case object Bid extends OrderType

/** Trying to sell bitcoins */
case object Ask extends OrderType

/** Request for an interchange. */
case class Order(orderType: OrderType, amount: BtcAmount, price: FiatAmount)

object Order {
  val AscendingPriceOrder: Ordering[Order] = Ordering.by[Order, BigDecimal](_.price.amount)
  val DescendingPriceOrder: Ordering[Order] = Ordering.by[Order, BigDecimal](-_.price.amount)
}
