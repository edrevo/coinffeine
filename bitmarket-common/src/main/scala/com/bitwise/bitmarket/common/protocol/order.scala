package com.bitwise.bitmarket.common.protocol

import com.bitwise.bitmarket.common.PeerConnection
import com.bitwise.bitmarket.common.currency.{FiatAmount, BtcAmount}

/** Request for an interchange */
sealed trait Order {
  val amount: BtcAmount
  val price: FiatAmount
  val requester: String
}

/** Requester is willing to buy `amount` BTC at price `price` */
case class Bid(
    override val amount: BtcAmount,
    override val price: FiatAmount,
    override val requester: String) extends Order

object Bid {
  implicit val Order = Ordering.by[Bid, BigDecimal](bid => -bid.price.amount)
}

/** Requester is willing to sell `amount` BTC at price `price` */
case class Ask(
    override val amount: BtcAmount,
    override val price: FiatAmount,
    override val requester: String) extends Order

object Ask {
  implicit val Order = Ordering.by[Ask, BigDecimal](ask => ask.price.amount)
}
