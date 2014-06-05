package com.coinffeine.market

import com.coinffeine.common.PeerConnection
import com.coinffeine.common.currency.BtcAmount
import com.coinffeine.common.protocol.messages.brokerage.{Ask, Bid, OrderType}

/** Bidding or asking position taken by a requester */
case class Position[T <: OrderType](
    orderType: T, amount: BtcAmount, price: Price, requester: PeerConnection) {

  /** Folds any Position type into a value of type T.
    *
    * @param bid       Transformation for bid positions
    * @param ask       Transformation for ask positions
    * @tparam R        Return type
    * @return          Transformed input
    */
  def fold[R](bid: Position[Bid.type] => R, ask: Position[Ask.type] => R): R =
    orderType match {
      case _: Bid.type => bid(this.asInstanceOf[Position[Bid.type]])
      case _: Ask.type => ask(this.asInstanceOf[Position[Ask.type]])
    }
}

object Position {

  def bid(amount: BtcAmount, price: Price, requester: PeerConnection): Position[Bid.type] =
    Position(Bid, amount, price, requester)

  def ask(amount: BtcAmount, price: Price, requester: PeerConnection): Position[Ask.type] =
    Position(Ask, amount, price, requester)
}

