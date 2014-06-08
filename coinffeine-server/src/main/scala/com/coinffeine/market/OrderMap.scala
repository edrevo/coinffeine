package com.coinffeine.market

import java.util.Currency
import scala.annotation.tailrec
import scala.collection.immutable.{SortedMap, TreeMap}

import com.coinffeine.common.currency.BtcAmount
import com.coinffeine.common.currency.Implicits._
import com.coinffeine.common.protocol.messages.brokerage.OrderType

/** Data structure that holds orders sorted by price and, within a given price, keep
  * them sorted with a FIFO policy. */
case class OrderMap[T <: OrderType] (
    orderType: T, currency: Currency, tree: SortedMap[Price, OrderQueue]) {

  def addPosition(position: Position[T]): OrderMap[T] = {
    require(position.price.currency == currency,
      s"Cannot mix $currency with ${position.price.currency}")
    val queue = tree.getOrElse(position.price, Seq.empty)
    val updatedQueue = queue :+ (position.amount, position.requester)
    copy(tree = tree.updated(position.price, updatedQueue))
  }

  /** Sorted client positions */
  def positions: Iterable[Position[T]] = for {
    (price, queue) <- tree.seq
    (amount, clientId) <- queue
  } yield Position(orderType, amount, price, clientId)

  def userPositions(userId: ClientId): Seq[Position[T]] =
    positions.filter(_.requester == userId).toSeq

  def firstPosition: Option[Position[T]] = positions.headOption

  def firstPrice: Option[Price] = tree.headOption.map(_._1)

  /** Remove an amount of BTC honoring order priority */
  def removeAmount(amount: BtcAmount): OrderMap[T] = copy(tree = removeAmountFromTree(amount, tree))

  /** Cancel a position.
    *
    * If the client has several identical positions, the least prioritized one is removed.
    *
    * @param position  Position to cancel
    * @return          New order map
    */
  def cancelPosition(position: Position[T]): OrderMap[T] = {
    val value = (position.amount, position.requester)
    tree.get(position.price) match {
      case Some(Seq(`value`)) => copy(tree = tree - position.price)
      case Some(queue) if queue.contains(value) =>
        val newQueue = removeLastOccurrence(value, queue)
        copy(tree = tree.updated(position.price, newQueue))
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

  def cancelPositions(requester: ClientId): OrderMap[T] =
    copy(tree = removeEmptyQueues(removeByRequester(requester, tree)))

  private def removeByRequester(requester: ClientId, tree: SortedMap[Price, OrderQueue]) =
    tree.mapValues(queue => queue.filterNot(_._2 == requester))

  private def removeEmptyQueues(tree: SortedMap[Price, OrderQueue]) = tree.filter {
    case (_, queue) => !queue.isEmpty
  }
}

object OrderMap {

  /** Empty order map */
  def empty[T <: OrderType](orderType: T, currency: Currency): OrderMap[T] =
    OrderMap(orderType, currency, TreeMap.empty(orderType.legacyPriceOrdering))

  def apply[T <: OrderType](first: Position[T], other: Position[T]*): OrderMap[T] = {
    val positions = first +: other
    val accumulator: OrderMap[T] = empty(first.orderType, orderCurrency(positions))
    positions.foldLeft(accumulator)(_.addPosition(_))
  }

  private def orderCurrency(positions: Seq[Position[_]]): Currency =
    requireSingleValue(positions.map(_.price.currency))

  private def requireSingleValue[T](values: Seq[T]): T = values.distinct.toList match {
    case List() => throw new IllegalArgumentException("At least one value required")
    case List(singleValue) => singleValue
    case multipleValues =>
      val names = multipleValues.map(_.toString).sorted
      throw new IllegalArgumentException(names.mkString("Cannot mix ", " and ", " values"))
  }
}
