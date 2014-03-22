package com.coinffeine.broker

import java.util.Currency
import scala.concurrent.duration._
import scala.language.postfixOps
import scala.math.max
import scala.util.Random

import akka.actor._

import com.coinffeine.common.PeerConnection
import com.coinffeine.common.currency.FiatAmount
import com.coinffeine.common.protocol._
import com.coinffeine.common.protocol.gateway.MessageGateway._
import com.coinffeine.common.protocol.messages.brokerage._
import com.coinffeine.market._

/** A broker actor maintains the order book of BTC trading on a given fiat currency. */
private[broker] class BrokerActor(
    currency: Currency,
    gateway: ActorRef,
    handshakeArbiterProps: Props,
    orderExpirationInterval: Duration) extends Actor with ActorLogging {

  private var book = OrderBook.empty(currency)
  private var expirationTimes = Map[PeerConnection, FiniteDuration]()
  private var lastPrice: Option[FiatAmount] = None

  override def preStart(): Unit = gateway ! Subscribe {
    case ReceiveMessage(order: Order, _) if order.price.currency == currency => true
    case ReceiveMessage(quoteRequest: QuoteRequest, _)
      if quoteRequest.currency == currency => true
    case ReceiveMessage(OrderCancellation(`currency`), _) => true
    case _ => false
  }

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
        notifyOrderMatch(orderMatch)
        context.actorOf(handshakeArbiterProps) ! orderMatch
      }
      crosses.lastOption.foreach { cross => lastPrice = Some(cross.price) }
      if (book.positions.exists(_.requester == requester)) {
        setExpirationFor(requester)
      }

    case ReceiveMessage(QuoteRequest(_), requester) =>
      gateway ! ForwardMessage(Quote(currency, book.spread, lastPrice), requester)

    case ReceiveMessage(OrderCancellation(_), requester) =>
      log.info(s"Order of $requester is cancelled")
      book = book.cancelOrder(requester)

    case ReceiveTimeout => expireOrders()
  }

  private def notifyOrderMatch(orderMatch: OrderMatch): Unit = {
    gateway ! ForwardMessage(orderMatch, orderMatch.buyer)
    gateway ! ForwardMessage(orderMatch, orderMatch.seller)
  }

  private def setExpirationFor(requester: PeerConnection): Unit = {
    if (orderExpirationInterval.isFinite()) {
      val expiration =
        (System.currentTimeMillis() millis) + orderExpirationInterval.asInstanceOf[FiniteDuration]
      expirationTimes = expirationTimes.updated(requester, expiration)
    }
  }

  private def scheduleNextExpiration(): Unit = {
    val timeout =
      if (expirationTimes.isEmpty || !orderExpirationInterval.isFinite) Duration.Inf
      else max(0, expirationTimes.values.min.toMillis - System.currentTimeMillis).millis
    context.setReceiveTimeout(timeout)
  }

  private def expireOrders(): Unit = {
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

object BrokerActor {
  trait Component {

    /** Props for creating a broker actor given some dependencies.
      *
      * On the event of an order match it creates a child actor from `handshakeArbiterProps` to
      * manage the handshake and let the parties to publish their commitments at the same time.
      *
      * @param currency  Currency to be traded for
      * @param gateway   Message gateway
      * @param handshakeArbiterProps  Props of the actor to take care of new exchange handshakes.
      * @param orderExpirationInterval  Time that orders take to be discarded if not renewed.
      */
    def brokerActorProps(
        currency: Currency,
        gateway: ActorRef,
        handshakeArbiterProps: Props,
        orderExpirationInterval: Duration = 1 minute) =
      Props(new BrokerActor(currency, gateway, handshakeArbiterProps, orderExpirationInterval))
  }
}
