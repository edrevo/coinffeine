package com.bitwise.bitmarket.broker

import java.util.Currency
import scala.concurrent.duration._
import scala.language.postfixOps
import scala.math.max

import akka.actor._

import com.bitwise.bitmarket.broker.BrokerActor._
import com.bitwise.bitmarket.market._
import com.bitwise.bitmarket.common.currency.FiatAmount
import com.bitwise.bitmarket.common.protocol.{Quote, Order}

class BrokerActor(currency: Currency, orderExpirationInterval: Duration = 60 seconds)
  extends Actor with ActorLogging {

  private var book = OrderBook.empty(currency)
  private var expirationTimes = Map[String, Long]()
  private var lastPrice: Option[FiatAmount] = None

  def receive: Receive = {

    case OrderPlacement(order) if order.price.currency != currency =>
      log.error("Dropping order not placed in %s: %s", currency, order)
      scheduleNextExpiration()

    case OrderPlacement(order) if book.orders.contains(order) =>
      setExpirationFor(order.requester)
      scheduleNextExpiration()

    case OrderPlacement(order) =>
      log.info("Order placed " + order)
      val (clearedBook, crosses) = book.placeOrder(order).clearMarket
      book = clearedBook
      crosses.foreach { cross =>
        sender ! NotifyCross(cross)
        lastPrice = Some(cross.price)
      }
      setExpirationFor(order.requester)
      scheduleNextExpiration()

    case QuoteRequest =>
      sender ! QuoteResponse(Quote(book.spread, lastPrice))
      scheduleNextExpiration()

    case OrderCancellation(requester) =>
      log.info(s"Order of $requester is cancelled")
      book = book.cancelOrder(requester)
      scheduleNextExpiration()

    case ReceiveTimeout =>
      expireOrders()
      scheduleNextExpiration()
  }

  private def setExpirationFor(requester: String) {
    val expiration = System.currentTimeMillis() + orderExpirationInterval.toMillis
    expirationTimes = expirationTimes.updated(requester, expiration)
  }

  private def scheduleNextExpiration() {
    val timeout =
      if (expirationTimes.isEmpty) Duration.Inf
      else max(0, expirationTimes.values.min - System.currentTimeMillis()).millis
    context.setReceiveTimeout(timeout)
  }

  private def expireOrders() {
    val currentTime = System.currentTimeMillis()
    val expired = expirationTimes.collect {
      case (requester, expirationTime) if expirationTime <= currentTime => requester
    }
    log.info("Expiring orders of " + expired.mkString(", "))
    book = expired.foldLeft(book)(_.cancelOrder(_))
    expirationTimes --= expired
  }
}

object BrokerActor {
  case object QuoteRequest
  case class QuoteResponse(quote: Quote)
  case class OrderPlacement(order: Order)
  case class OrderCancellation(requester: String)
  case class NotifyCross(cross: Cross)
}
