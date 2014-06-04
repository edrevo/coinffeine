package com.coinffeine.market

import java.util.Currency
import scala.annotation.tailrec

import com.coinffeine.common.PeerConnection
import com.coinffeine.common.protocol.Spread
import com.coinffeine.common.protocol.messages.brokerage.{Ask, Bid}

/** Represents a snapshot of a continuous double auction (CDA) */
case class OrderBook(bids: BidMap, asks: AskMap) {

  require(bids.currency == asks.currency)

  def positions: Iterable[Position[_]] = bids.positions ++ asks.positions

  def userPositions(userId: PeerConnection): Seq[Position[_]] =
    bids.userPositions(userId) ++ asks.userPositions(userId)

  /** Tells if a transaction is possible with current orders. */
  def isCrossed: Boolean = spread match {
    case (Some(bidPrice), Some(askPrice)) if bidPrice >= askPrice => true
    case _ => false
  }

  /** Get current spread (interval between the highest bet price to the lowest bid price */
  def spread: Spread = highestBid -> lowestAsk

  def highestBid: Option[Price] = bids.firstPrice

  def lowestAsk: Option[Price] = asks.firstPrice

  /** Add a new position
    *
    * @param position  Position to add
    * @return          New order book
    */
  def addPosition(position: Position[_]): OrderBook =
    position.fold(bid = addBidPosition, ask = addAskPosition)

  def addPositions(positions: Seq[Position[_]]): OrderBook =
    positions.foldLeft(this)(_.addPosition(_))

  def addBidPosition(position: Position[Bid.type]): OrderBook =
    copy(bids = bids.addPosition(position))

  def addAskPosition(position: Position[Ask.type]): OrderBook =
    copy(asks = asks.addPosition(position))

  /** Cancel a position.
    *
    * If the client has several identical positions, the least prioritized one is removed.
    *
    * @param position  Position to cancel
    * @return          New order book
    */
  def cancelPosition(position: Position[_]): OrderBook =
    position.fold(bid = cancelBidPosition, ask = cancelAskPosition)

  def cancelBidPosition(position: Position[Bid.type]): OrderBook =
    copy(bids = bids.cancelPosition(position))

  def cancelAskPosition(position: Position[Ask.type]): OrderBook =
    copy(asks = asks.cancelPosition(position))

  def cancelPositions(positions: Seq[Position[_]]): OrderBook =
    positions.foldLeft(this)(_.cancelPosition(_))

  /** Cancel al orders of a given client */
  def cancelAllPositions(requester: ClientId): OrderBook =
    copy(bids = bids.cancelPositions(requester), asks = asks.cancelPositions(requester))

  /** Clear the market by crossing bid and ask orders
    *
    * @return Cleared market and a sequence of crosses
    */
  def clearMarket: (OrderBook, Seq[Cross]) = clearMarket(bids, asks, Seq.empty)

  @tailrec
  private def clearMarket(bids: BidMap, asks: AskMap, crosses: Seq[Cross]): (OrderBook, Seq[Cross]) =
    (bids.firstPrice, asks.firstPrice) match {
      case (Some(bidPrice), Some(askPrice)) if bidPrice >= askPrice =>
        val (cross, remainingBids, remainingAsks) = crossOrders(bids, asks)
        clearMarket(remainingBids, remainingAsks, crosses :+ cross)
      case _ => (OrderBook(bids, asks), crosses)
    }

  private def crossOrders(bids: BidMap, asks: AskMap): (Cross, BidMap, AskMap) = {
    val bid = bids.firstPosition.get
    val ask = asks.firstPosition.get
    val crossedAmount = bid.amount min ask.amount
    val cross = Cross(
      amount = crossedAmount,
      price = (bid.price + ask.price) / 2,
      buyer = bid.requester,
      seller = ask.requester
    )
    (cross, bids.removeAmount(crossedAmount), asks.removeAmount(crossedAmount))
  }
}

object OrderBook {

  def apply(position: Position[_], otherPositions: Position[_]*): OrderBook =
    empty(position.price.currency).addPositions(position +: otherPositions)

  def empty(currency: Currency): OrderBook = OrderBook(
    bids = OrderMap.empty(Bid, currency),
    asks = OrderMap.empty(Ask, currency)
  )
}
