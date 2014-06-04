package com.coinffeine.market

import java.util.Currency
import scala.annotation.tailrec

import com.coinffeine.common.protocol.Spread
import com.coinffeine.common.protocol.messages.brokerage.{Ask, Bid, Order}

/** Represents a snapshot of a continuous double auction (CDA) */
case class OrderBook(bids: OrderMap, asks: OrderMap) {

  require(bids.currency == asks.currency)
  require(bids.orderType == Bid && asks.orderType == Ask)

  def positions: Iterable[Position] = bids.positions ++ asks.positions

  /** Tells if a transaction is possible with current orders. */
  def isCrossed: Boolean = spread match {
    case (Some(bidPrice), Some(askPrice)) if bidPrice >= askPrice => true
    case _ => false
  }

  /** Get current spread (interval between the highest bet price to the lowest bid price */
  def spread: Spread = highestBid -> lowestAsk

  def highestBid: Option[Price] = bids.firstPrice

  def lowestAsk: Option[Price] = asks.firstPrice

  /** Place a new order.
    *
    * @param requester  Who is placing the order
    * @param order      Ask or Bid to place
    * @return           New order book
    */
  def placeOrder(requester: ClientId, order: Order): OrderBook =
    addPosition(Position(requester, order))

  /** Add a new position */
  def addPosition(position: Position): OrderBook = position.order.orderType match {
    case Bid => copy(bids = bids.addPosition(position))
    case Ask => copy(asks = asks.addPosition(position))
  }

  /** Cancel a position.
    *
    * If the client has several identical positions, the least prioritized one is removed.
    *
    * @param position  Position to cancel
    * @return          New order book
    */
  def cancelPosition(position: Position): OrderBook = position.order.orderType match {
    case Bid => copy(bids = bids.cancelPosition(position))
    case Ask => copy(asks = asks.cancelPosition(position))
  }

  /** Cancel al orders of a given client */
  def cancelPositions(requester: ClientId): OrderBook =
    copy(bids = bids.cancelPositions(requester), asks = asks.cancelPositions(requester))

  /** Clear the market by crossing bid and ask orders
    *
    * @return Cleared market and a sequence of crosses
    */
  def clearMarket: (OrderBook, Seq[Cross]) = clearMarket(bids, asks, Seq.empty)

  @tailrec
  private def clearMarket(bids: OrderMap, asks: OrderMap, crosses: Seq[Cross]): (OrderBook, Seq[Cross]) =
    (bids.firstPrice, asks.firstPrice) match {
      case (Some(bidPrice), Some(askPrice)) if bidPrice >= askPrice =>
        val (cross, remainingBids, remainingAsks) = crossOrders(bids, asks)
        clearMarket(remainingBids, remainingAsks, crosses :+ cross)
      case _ => (OrderBook(bids, asks), crosses)
    }

  private def crossOrders(bids: OrderMap, asks: OrderMap): (Cross, OrderMap, OrderMap) = {
    val bid = bids.firstPosition.get
    val ask = asks.firstPosition.get
    val crossedAmount = bid.order.amount min ask.order.amount
    val cross = Cross(
      amount = crossedAmount,
      price = (bid.order.price + ask.order.price) / 2,
      buyer = bid.requester,
      seller = ask.requester
    )
    (cross, bids.removeAmount(crossedAmount), asks.removeAmount(crossedAmount))
  }
}

object OrderBook {
  def apply(position: Position, otherPositions: Position*): OrderBook = {
    val accum = empty(position.order.price.currency)
    val positions = position +: otherPositions
    positions.foldLeft(accum)(_.addPosition(_))
  }

  def empty(currency: Currency): OrderBook = OrderBook(
    bids = OrderMap.empty(Bid, currency),
    asks = OrderMap.empty(Ask, currency)
  )
}
