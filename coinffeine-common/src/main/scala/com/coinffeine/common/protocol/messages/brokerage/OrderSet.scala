package com.coinffeine.common.protocol.messages.brokerage

import com.coinffeine.common.currency.{BtcAmount, FiatAmount}
import com.coinffeine.common.protocol.messages.PublicMessage

/** Represents the set of orders placed by a peer.
  *
  * @param market   Market in which orders are placed
  * @param bids     Bid orders
  * @param asks     Ask orders
  */
case class OrderSet(market: Market, bids: VolumeByPrice, asks: VolumeByPrice) extends PublicMessage {

  requireSingleCurrency()
  requireNotCrossed()

  def highestBid: Option[FiatAmount] = bids.highestPrice
  def lowestAsk: Option[FiatAmount] = asks.lowestPrice

  def isEmpty = bids.isEmpty && asks.isEmpty

  def addOrder(orderType: OrderType, amount: BtcAmount, price: FiatAmount): OrderSet =
    orderType match {
      case Bid => copy(bids = bids.increase(price, amount))
      case Ask => copy(asks = asks.increase(price, amount))
    }

  def cancelOrder(orderType: OrderType, amount: BtcAmount, price: FiatAmount): OrderSet =
    orderType match {
      case Bid => copy(bids = bids.decrease(price, amount))
      case Ask => copy(asks = asks.decrease(price, amount))
    }

  private def requireSingleCurrency(): Unit = {
    require(bids.currency == asks.currency && asks.currency == market.currency, "Mixed currencies")
  }

  private def requireNotCrossed(): Unit = {
    val priceCrossed = (highestBid, lowestAsk) match {
      case (Some(bid), Some(ask)) if bid >= ask => true
      case _ => false
    }
    require(!priceCrossed, s"Bids and asks are crossed")
  }
}

object OrderSet {
  def empty(market: Market): OrderSet =
    OrderSet(market, VolumeByPrice.empty(market.currency), VolumeByPrice.empty(market.currency))
}
