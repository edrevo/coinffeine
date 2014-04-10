package com.coinffeine.broker

import java.util.Currency
import scala.concurrent.duration.Duration
import scala.util.Random

import akka.actor._

import com.coinffeine.arbiter.HandshakeArbiterActor
import com.coinffeine.broker.BrokerActor.StartBrokering
import com.coinffeine.common.PeerConnection
import com.coinffeine.common.currency.FiatAmount
import com.coinffeine.common.protocol.ProtocolConstants
import com.coinffeine.common.protocol.gateway.MessageGateway._
import com.coinffeine.common.protocol.messages.brokerage._
import com.coinffeine.market._

/** A broker actor maintains the order book of BTC trading on a given fiat currency. */
private[broker] class BrokerActor(
    handshakeArbiterProps: Props,
    orderExpirationInterval: Duration) extends Actor with ActorLogging {

  override def receive: Receive = {
    case StartBrokering(currency, gateway) =>
      new InitializedBroker(currency, gateway).startBrokering()
  }

  private class InitializedBroker(currency: Currency, gateway: ActorRef) {
    private var book = OrderBook.empty(currency)
    private val orderTimeouts = new ExpirationSchedule[PeerConnection]
    private var lastPrice: Option[FiatAmount] = None

    def startBrokering(): Unit = {
      subscribeToMessages()
      context.become(processMessage.andThen(_ => scheduleNextExpiration()))
    }

    private def subscribeToMessages(): Unit = gateway ! Subscribe {
      case ReceiveMessage(order: Order, _) if order.price.currency == currency => true
      case ReceiveMessage(quoteRequest: QuoteRequest, _)
        if quoteRequest.currency == currency => true
      case ReceiveMessage(CancelOrder(`currency`), _) => true
      case _ => false
    }

    private def processMessage: Receive = {
      case ReceiveMessage(order: Order, _) if order.price.currency != currency =>
        log.error("Dropping order not placed in %s: %s", currency, order)

      case ReceiveMessage(order: Order, requester)
        if book.positions.contains(Position(requester, order)) =>
        orderTimeouts.setExpirationFor(requester, orderExpirationInterval)

      case ReceiveMessage(order: Order, requester) =>
        log.info("Order placed " + order)
        val (clearedBook, crosses) = book.placeOrder(requester, order).clearMarket(idGenerator)
        book = clearedBook
        crosses.foreach {
          orderMatch =>
            context.actorOf(handshakeArbiterProps) ! orderMatch
        }
        crosses.lastOption.foreach {
          cross => lastPrice = Some(cross.price)
        }
        if (book.positions.exists(_.requester == requester)) {
          orderTimeouts.setExpirationFor(requester, orderExpirationInterval)
        }

      case ReceiveMessage(QuoteRequest(_), requester) =>
        gateway ! ForwardMessage(Quote(currency, book.spread, lastPrice), requester)

      case ReceiveMessage(CancelOrder(_), requester) =>
        log.info(s"Order of $requester is cancelled")
        book = book.cancelOrder(requester)

      case ReceiveTimeout => expireOrders()
    }

    private def scheduleNextExpiration(): Unit =
      context.setReceiveTimeout(orderTimeouts.timeToNextExpiration())

    private def expireOrders(): Unit = {
      val expired = orderTimeouts.removeExpired()
      book = expired.foldLeft(book)(_.cancelOrder(_))
      log.info("Expiring orders of " + expired.mkString(", "))
    }

    private def idGenerator = Stream.continually(Random.nextLong().toString)
  }
}

object BrokerActor {

  /** Start brokering exchanges on a given currency. */
  case class StartBrokering(currency: Currency, gateway: ActorRef)

  trait Component { this: HandshakeArbiterActor.Component with ProtocolConstants.Component =>

    lazy val brokerActorProps: Props =
      Props(new BrokerActor(handshakeArbiterProps, protocolConstants.orderExpirationInterval))
  }
}
