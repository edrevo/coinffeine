package com.coinffeine.market

import java.util.Currency
import scala.annotation.tailrec
import scala.collection.immutable.{SortedMap, TreeMap}

import com.coinffeine.common.currency.{BtcAmount, FiatAmount}
import com.coinffeine.common.currency.Implicits._
import com.coinffeine.common.protocol.messages.brokerage.{Ask, Bid, Order, OrderType}

/** Data structure that holds orders sorted by price and, within a given price, keep
  * them sorted with a FIFO policy. */
case class OrderMap private (orderType: OrderType, currency: Currency, tree: SortedMap[Price, OrderQueue]) {

  def addPosition(position: Position): OrderMap = {
    require(position.order.orderType == orderType,
      s"Cannot mix $orderType with ${position.order.orderType}")
    require(position.order.price.currency == currency,
      s"Cannot mix $currency with ${position.order.price.currency}")
    val queue = tree.getOrElse(position.order.price, Seq.empty)
    val updatedQueue = queue :+ (position.order.amount, position.requester)
    copy(tree = tree.updated(position.order.price, updatedQueue))
  }

  /** Sorted client positions */
  def positions: Iterable[Position] = for {
    (price, queue) <- tree.seq
    (amount, clientId) <- queue
  } yield Position(clientId, Order(orderType, amount, price))

  def firstPosition: Option[Position] = positions.headOption

  def firstPrice: Option[Price] = tree.headOption.map(_._1)

  /** Remove an amount of BTC honoring order priority */
  def removeAmount(amount: BtcAmount): OrderMap = copy(tree = removeAmountFromTree(amount, tree))

  /** Cancel a position.
    *
    * If the client has several identical positions, the least prioritized one is removed.
    *
    * @param position  Position to cancel
    * @return          New order map
    */
  def cancelPosition(position: Position): OrderMap = {
    require(position.order.orderType == orderType)
    val value = (position.order.amount, position.requester)
    tree.get(position.order.price) match {
      case Some(Seq(`value`)) => copy(tree = tree - position.order.price)
      case Some(queue) if queue.contains(value) =>
        val newQueue = removeLastOccurrence(value, queue)
        copy(tree = tree.updated(position.order.price, newQueue))
      case _ => this
    }
  }

  private def removeLastOccurrence(value: (BtcAmount, ClientId), queue: OrderQueue): OrderQueue =
    queue.patch(queue.lastIndexOf(value), Nil, 1)

  @tailrec
  private def removeAmountFromTree(
      amount: BtcAmount, tree: SortedMap[Price, OrderQueue]): SortedMap[Price, OrderQueue] =
    if (amount <= 0.BTC) tree
    else {
      require(!tree.isEmpty, s"Cannot remove $amount from $tree")
      val (price, queue) = tree.head
      val (remainingAmount, remainingQueue) = removeAmountFromQueue(amount, queue)
      val remainingTree =
        if (remainingQueue.isEmpty) tree - price else tree.updated(price, remainingQueue)
      removeAmountFromTree(remainingAmount, remainingTree)
    }

  @tailrec
  private def removeAmountFromQueue(amount: BtcAmount, queue: OrderQueue): (BtcAmount, OrderQueue) =
    if (amount <= 0.BTC) (0.BTC, queue)
    else queue match {
      case Seq() =>
        (amount, queue)
      case Seq((firstAmount, _), rest @ _*) if firstAmount <= amount =>
        removeAmountFromQueue(amount - firstAmount, rest)
      case Seq((firstAmount, id), rest @ _*) =>
        (0.BTC, (firstAmount - amount, id) +: rest)
    }

  def cancelPositions(requester: ClientId): OrderMap =
    copy(tree = removeEmptyQueues(removeByRequester(requester, tree)))

  private def removeByRequester(requester: ClientId, tree: SortedMap[Price, OrderQueue]) =
    tree.mapValues(queue => queue.filterNot(_._2 == requester))

  private def removeEmptyQueues(tree: SortedMap[Price, OrderQueue]) = tree.filter {
    case (_, queue) => !queue.isEmpty
  }
}

object OrderMap {

  /** Empty order map */
  def empty(orderType: OrderType, currency: Currency): OrderMap =
    OrderMap(orderType, currency, TreeMap.empty(OrderMap.Orderings(orderType)))

  def apply(first: Position, other: Position*): OrderMap = {
    val positions = first +: other
    val accumulator = empty(orderType(positions), orderCurrency(positions))
    positions.foldLeft(accumulator)(_.addPosition(_))
  }

  private def orderType(positions: Seq[Position]): OrderType =
    requireSingleValue(positions.map(_.order.orderType))

  private def orderCurrency(positions: Seq[Position]): Currency =
    requireSingleValue(positions.map(_.order.price.currency))

  private def requireSingleValue[T](values: Seq[T]): T = values.distinct.toList match {
    case List() => throw new IllegalArgumentException("At least one value required")
    case List(singleValue) => singleValue
    case multipleValues =>
      val names = multipleValues.map(_.toString).sorted
      throw new IllegalArgumentException(names.mkString("Cannot mix ", " and ", " values"))
  }

  private val Orderings: Map[OrderType, Ordering[Price]] = Map(
    Bid -> Ordering.by[FiatAmount, BigDecimal](x => -x.amount),
    Ask -> FiatAmount
  )
}
