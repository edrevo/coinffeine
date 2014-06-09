package com.coinffeine.market

import com.coinffeine.common.{BitcoinAmount, FiatCurrency, PeerConnection}
import com.coinffeine.common.protocol.messages.brokerage.{Ask, Bid, OrderType}

/** Bidding or asking position taken by a requester */
case class Position[T <: OrderType, C <: FiatCurrency](
    orderType: T, amount: BitcoinAmount, price: Price[C], requester: PeerConnection) {

  /** Folds any Position type into a value of type T.
    *
    * @param bid       Transformation for bid positions
    * @param ask       Transformation for ask positions
    * @tparam R        Return type
    * @return          Transformed input
    */
  def fold[R](bid: Position[Bid.type, C] => R, ask: Position[Ask.type, C] => R): R =
    orderType match {
      case _: Bid.type => bid(this.asInstanceOf[Position[Bid.type, C]])
      case _: Ask.type => ask(this.asInstanceOf[Position[Ask.type, C]])
    }
}

object Position {

  def bid[C <: FiatCurrency](
      amount: BitcoinAmount,
      price: Price[C],
      requester: PeerConnection): Position[Bid.type, C] =
    Position(Bid, amount, price, requester)

  def ask[C <: FiatCurrency](
      amount: BitcoinAmount,
      price: Price[C],
      requester: PeerConnection): Position[Ask.type, C] =
    Position(Ask, amount, price, requester)
}

