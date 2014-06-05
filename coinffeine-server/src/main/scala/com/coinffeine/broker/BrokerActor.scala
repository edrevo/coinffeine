package com.coinffeine.broker

import scala.concurrent.duration.Duration

import akka.actor._

import com.coinffeine.arbiter.HandshakeArbiterActor
import com.coinffeine.broker.BrokerActor.BrokeringStart
import com.coinffeine.common.PeerConnection
import com.coinffeine.common.currency.FiatAmount
import com.coinffeine.common.protocol.ProtocolConstants
import com.coinffeine.common.protocol.gateway.MessageGateway._
import com.coinffeine.common.protocol.messages.brokerage._
import com.coinffeine.market._

/** A broker actor maintains the order book of BTC trading on a given market. */
private[broker] class BrokerActor(
    handshakeArbiterProps: Props,
    orderExpirationInterval: Duration) extends Actor with ActorLogging {

  override def receive: Receive = {
    case BrokeringStart(market, gateway) =>
      new InitializedBroker(market, gateway).startBrokering()
  }

  private class InitializedBroker(market: Market, gateway: ActorRef) {
    private var book = OrderBook.empty(market.currency)
    private val orderTimeouts = new ExpirationSchedule[PeerConnection]
    private var lastPrice: Option[FiatAmount] = None

    def startBrokering(): Unit = {
      subscribeToMessages()
      context.become(processMessage.andThen(_ => scheduleNextExpiration()))
    }

    private def subscribeToMessages(): Unit = gateway ! Subscribe {
      case ReceiveMessage(orders: OrderSet, _) if orders.market == market => true
      case ReceiveMessage(quoteRequest: QuoteRequest, _)
        if quoteRequest.currency == market.currency => true
      case _ => false
    }

    private def processMessage: Receive = {

      case ReceiveMessage(orders: OrderSet, requester: PeerConnection) =>
        orderTimeouts.setExpirationFor(requester, orderExpirationInterval)
        log.info(s"Updating orders of $requester")
        log.debug(s"Orders for $requester: $orders")
        updateUserOrders(orders, requester)
        clearMarket()

      case ReceiveMessage(QuoteRequest(_), requester) =>
        gateway ! ForwardMessage(Quote(market.currency, book.spread, lastPrice), requester)

      case ReceiveTimeout => expireOrders()
    }

    private def updateUserOrders(orders: OrderSet, requester: PeerConnection): Unit = {
      val existingPositions = book.userPositions(requester)
      val currentPositions = orderSetPositions(orders, requester)
      val positionsToRemove = existingPositions.diff(currentPositions)
      val positionsToAdd = currentPositions.diff(existingPositions)
      book = book.cancelPositions(positionsToRemove).addPositions(positionsToAdd)
    }

    private def orderSetPositions(orders: OrderSet, requester: PeerConnection): Seq[Position[_]] = {
      def toPositions(orderType: OrderType, entries: Seq[OrderSet.Entry]) =
        (for (OrderSet.Entry(amount, price) <- entries)
          yield Position(orderType, amount, price, requester)).toSeq

      toPositions(Bid, orders.bids) ++ toPositions(Ask, orders.asks)
    }

    private def clearMarket(): Unit = {
      val (clearedBook, crosses) = book.clearMarket
      book = clearedBook
      if (!crosses.isEmpty) {
        startArbiters(crosses)
        lastPrice = Some(crosses.last.price)
      }
    }

    private def startArbiters(crosses: Seq[Cross]): Unit = {
      crosses.foreach { cross =>
        val orderMatch = cross.toOrderMatch(randomId())
        context.actorOf(handshakeArbiterProps) ! orderMatch
      }
    }

    private def scheduleNextExpiration(): Unit =
      context.setReceiveTimeout(orderTimeouts.timeToNextExpiration())

    private def expireOrders(): Unit = {
      val expired = orderTimeouts.removeExpired()
      book = expired.foldLeft(book)(_.cancelAllPositions(_))
      log.info("Expiring orders of " + expired.mkString(", "))
    }

    private val random = new scala.util.Random(new java.security.SecureRandom())
    private def randomId() = random.nextLong().abs.toString
  }
}

object BrokerActor {

  /** Start brokering exchanges on a given currency. */
  case class BrokeringStart(market: Market, gateway: ActorRef)

  trait Component { this: HandshakeArbiterActor.Component with ProtocolConstants.Component =>

    lazy val brokerActorProps: Props =
      Props(new BrokerActor(handshakeArbiterProps, protocolConstants.orderExpirationInterval))
  }
}
