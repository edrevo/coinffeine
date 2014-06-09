package com.coinffeine.common.protocol.messages.brokerage

import com.coinffeine.common.{CurrencyAmount, FiatCurrency, BitcoinAmount}
import com.coinffeine.common.protocol.messages.PublicMessage

/** Represents the set of orders placed by a peer.
  *
  * @param market   Market in which orders are placed
  * @param bids     Bid orders
  * @param asks     Ask orders
  */
case class OrderSet[+C <: FiatCurrency](
    market: Market[C], bids: VolumeByPrice[C], asks: VolumeByPrice[C]) extends PublicMessage {

  requireNotCrossed()

  def highestBid: Option[CurrencyAmount[C]] = bids.highestPrice
  def lowestAsk: Option[CurrencyAmount[C]] = asks.lowestPrice

  def isEmpty = bids.isEmpty && asks.isEmpty

  def addOrder[B >: C <: FiatCurrency](
      orderType: OrderType, amount: BitcoinAmount, price: CurrencyAmount[B]): OrderSet[B] =
    orderType match {
      case Bid => copy(bids = bids.increase(price, amount))
      case Ask => copy(asks = asks.increase(price, amount))
    }

  def cancelOrder[B >: C <: FiatCurrency](
      orderType: OrderType, amount: BitcoinAmount, price: CurrencyAmount[B]): OrderSet[B] =
    orderType match {
      case Bid => copy(bids = bids.decrease(price, amount))
      case Ask => copy(asks = asks.decrease(price, amount))
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
  def empty[C <: FiatCurrency](market: Market[C]): OrderSet[C] =
    OrderSet(market, VolumeByPrice.empty, VolumeByPrice.empty)
}
