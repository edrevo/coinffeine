package com.coinffeine.common.protocol.messages.brokerage

import com.coinffeine.common.currency.{BtcAmount, FiatAmount}
import com.coinffeine.common.protocol.messages.PublicMessage

/** Represents the set of orders placed by a peer.
  *
  * @param market   Market in which orders are placed
  * @param bids     Bid orders
  * @param asks     Ask orders
  */
case class OrderSet(
    market: Market,
    bids: Seq[OrderSet.Entry] = Seq.empty,
    asks: Seq[OrderSet.Entry] = Seq.empty) extends PublicMessage {

  requireSingleCurrency()
  requireNotCrossed()

  def highestBid: Option[FiatAmount] = bids.map(_.price).reduceOption(_ max _)
  def lowestAsk: Option[FiatAmount] = asks.map(_.price).reduceOption(_ min _)

  private def requireSingleCurrency(): Unit = {
    val currenciesInOrders = (bids ++ asks).map(_.price.currency).toSet
    require(currenciesInOrders.isEmpty || currenciesInOrders == Set(market.currency), "Mixed currencies")
  }

  private def requireNotCrossed(): Unit = {
    val priceCrossed = (highestBid, lowestAsk) match {
      case (Some(bid), Some(ask)) if bid >= ask => true
      case _ => false
    }
    require(!priceCrossed, "Bids and asks are crossed")
  }
}

object OrderSet {
  case class Entry(amount: BtcAmount, price: FiatAmount) {
    require(amount.amount > 0, "Amount ordered must be strictly positive")
    require(price.amount > 0, "Price must be strictly positive")
  }
}
