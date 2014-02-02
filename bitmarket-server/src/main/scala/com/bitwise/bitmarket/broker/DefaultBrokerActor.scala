package com.bitwise.bitmarket.broker

import java.util.Currency
import scala.concurrent.duration._
import scala.language.postfixOps
import scala.math.max
import scala.util.Random

import akka.actor._

import com.bitwise.bitmarket.common.PeerConnection
import com.bitwise.bitmarket.common.currency.FiatAmount
import com.bitwise.bitmarket.common.protocol._
import com.bitwise.bitmarket.common.protocol.gateway.MessageGateway.{ForwardMessage, ReceiveMessage}
import com.bitwise.bitmarket.market._

private[broker] class DefaultBrokerActor(
    currency: Currency,
    gateway: ActorRef,
    orderExpirationInterval: Duration) extends Actor with ActorLogging {

  private var book = OrderBook.empty(currency)
  private var expirationTimes = Map[PeerConnection, FiniteDuration]()
  private var lastPrice: Option[FiatAmount] = None

  override def receive: Receive = processMessage.andThen(_ => scheduleNextExpiration())

  private def processMessage: Receive = {
    case ReceiveMessage(order: Order, _) if order.price.currency != currency =>
      log.error("Dropping order not placed in %s: %s", currency, order)

    case ReceiveMessage(order: Order, requester)
      if book.positions.contains(Position(requester, order)) => setExpirationFor(requester)

    case ReceiveMessage(order: Order, requester) =>
      log.info("Order placed " + order)
      val (clearedBook, crosses) = book.placeOrder(requester, order).clearMarket(idGenerator)
      book = clearedBook
      crosses.foreach { orderMatch =>
        gateway ! ForwardMessage(orderMatch, orderMatch.buyer)
        gateway ! ForwardMessage(orderMatch, orderMatch.seller)
      }
      crosses.lastOption.foreach { cross => lastPrice = Some(cross.price) }
      if (book.positions.exists(_.requester == requester)) {
        setExpirationFor(requester)
      }

    case ReceiveMessage(QuoteRequest(_), requester) =>
      gateway ! ForwardMessage(Quote(book.spread, lastPrice), requester)

    case ReceiveMessage(OrderCancellation(_), requester) =>
      log.info(s"Order of $requester is cancelled")
      book = book.cancelOrder(requester)

    case ReceiveTimeout => expireOrders()
  }

  private def setExpirationFor(requester: PeerConnection) {
    if (orderExpirationInterval.isFinite) {
      val expiration =
        (System.currentTimeMillis() millis) + orderExpirationInterval.asInstanceOf[FiniteDuration]
      expirationTimes = expirationTimes.updated(requester, expiration)
    }
  }

  private def scheduleNextExpiration() {
    val timeout =
      if (expirationTimes.isEmpty || !orderExpirationInterval.isFinite) Duration.Inf
      else max(0, expirationTimes.values.min.toMillis - System.currentTimeMillis).millis
    context.setReceiveTimeout(timeout)
  }

  private def expireOrders() {
    val currentTime = System.currentTimeMillis() millis
    val expired = expirationTimes.collect {
      case (requester, expirationTime) if expirationTime <= currentTime => requester
    }
    log.info("Expiring orders of " + expired.mkString(", "))
    book = expired.foldLeft(book)(_.cancelOrder(_))
    expirationTimes --= expired
  }

  private def idGenerator = Stream.continually(Random.nextLong().toString)
}

object DefaultBrokerActor {
  trait Component extends BrokerActor.Component {
    override def brokerActorProps(
        currency: Currency, gateway: ActorRef, orderExpirationInterval: Duration) =
      Props(new DefaultBrokerActor(currency, gateway, orderExpirationInterval))
  }
}
