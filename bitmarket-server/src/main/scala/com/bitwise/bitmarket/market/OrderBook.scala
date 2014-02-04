package com.bitwise.bitmarket.market

import java.util.Currency
import scala.annotation.tailrec

import com.bitwise.bitmarket.common.protocol._
import com.bitwise.bitmarket.common.PeerConnection

/** Represents a snapshot of a continuous double auction (CDA) */
case class OrderBook(
  currency: Currency,
  bids: Seq[Position],
  asks: Seq[Position]) {

  requireSameCurrency()
  requireSortedOrders()
  requireSingleOrderPerRequester()

  def positions: Seq[Position] = bids ++ asks

  /** Tells if a transaction is possible with current orders. */
  def hasCross: Boolean = spread match {
    case (Some(highestBid), Some(lowestAsk)) if highestBid.amount >= lowestAsk.amount => true
    case _ => false
  }

  /** Get current spread (interval between the highest bet price to the lowest bid price */
  def spread: Spread = (bids.headOption.map(_.order.price), asks.headOption.map(_.order.price))

  /** Place a new order.
    *
    * Note that a preexisting order by the same requester will be replaced by the new one.
    *
    * @param requester  Who is placing the order
    * @param order      Ask or Bid to place
    * @return           New order book
    */
  def placeOrder(requester: PeerConnection, order: Order): OrderBook = {
    val position = Position(requester, order)
    val (newBid, newAsk) = order.orderType match {
      case Bid => (Some(position), None)
      case Ask => (None, Some(position))
    }
    copy(
      bids = sortBids(bids.filter(_.requester != requester) ++ newBid),
      asks = sortAsks(asks.filter(_.requester != requester) ++ newAsk)
    )
  }

  /** Cancel requester order if any. */
  def cancelOrder(requester: PeerConnection): OrderBook = copy(
    bids = bids.filter(_.requester != requester),
    asks = asks.filter(_.requester != requester)
  )

  /** Clear the market by crossing bid and ask orders
    *
    * @return Cleared market and a sequence of crosses
    */
  def clearMarket(idStream: Stream[String]): (OrderBook, Seq[OrderMatch]) =
    clearMarket(idStream, bids, asks, Seq.empty)

  @tailrec
  private def clearMarket(
      idStream: Stream[String],
      bids: Seq[Position],
      asks: Seq[Position],
      crosses: Seq[OrderMatch]): (OrderBook, Seq[OrderMatch]) = {
    (bids.headOption, asks.headOption) match {
      case (Some(bid), Some(ask)) if bid.order.price.amount >= ask.order.price.amount =>
        val (cross, remainingBid, remainingAsk) = crossOrders(idStream.head, bid, ask)
        clearMarket(
          idStream.tail,
          remainingBid.toList ++ bids.tail,
          remainingAsk.toList ++ asks.tail,
          crosses :+ cross
        )
      case _ => (OrderBook(currency, bids, asks), crosses)
    }
  }

  private def crossOrders(id: String, bid: Position, ask: Position):
      (OrderMatch, Option[Position], Option[Position]) = {
    val crossedAmount = bid.order.amount min ask.order.amount
    val remainingBid =
      if (bid.order.amount > crossedAmount) Some(bid.reduceAmount(crossedAmount)) else None
    val remainingAsk =
      if (ask.order.amount > crossedAmount) Some(ask.reduceAmount(crossedAmount)) else None
    val cross = OrderMatch(
      exchangeId = id,
      amount = crossedAmount,
      price = (bid.order.price + ask.order.price) / 2,
      buyer = bid.requester,
      seller = ask.requester
    )
    (cross, remainingBid, remainingAsk)
  }

  private def sortBids(bids: Seq[Position]) = bids.sortBy(_.order)

  private def sortAsks(asks: Seq[Position]) = asks.sortBy(_.order)

  private def requireSameCurrency() {
    val otherCurrency: Option[Currency] = positions.map(_.order.price.currency).find(_ != currency)
    require(
      otherCurrency.isEmpty,
      s"A currency (${otherCurrency.get}) other than $currency was used"
    )
  }

  private def requireSortedOrders() {
    require(sortBids(bids) == bids, "Bids must be sorted")
    require(sortAsks(asks) == asks, "Asks must be sorted")
  }

  private def requireSingleOrderPerRequester() {
    val requestersWithMultipleOrders = for {
      (requester, orders) <- positions.groupBy(_.requester)
      if orders.size > 1
    } yield requester
    require(requestersWithMultipleOrders.isEmpty,
      "Requesters with multiple orders: " + requestersWithMultipleOrders.mkString(", "))
  }
}

object OrderBook {
  def empty(currency: Currency) = OrderBook(currency, List.empty, List.empty)
}

